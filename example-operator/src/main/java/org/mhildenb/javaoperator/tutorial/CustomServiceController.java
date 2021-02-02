package org.mhildenb.javaoperator.tutorial;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A very simple sample controller that creates a service with a label. */
@Controller
public class CustomServiceController implements ResourceController<CustomService> {

  private static final Logger log = LoggerFactory.getLogger(CustomServiceController.class);

  private final KubernetesClient kubernetesClient;

  private Map<CustomService, Watch> svcWatches = new HashMap<CustomService, Watch>();

  public CustomServiceController(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  private void setupServiceWatcher(CustomService customResource) {
    // shouldn't be setting up multiple watches
    assert (!svcWatches.containsKey(customResource));

    String name = customResource.getMetadata().getName();

    Watch watch = kubernetesClient.services().inNamespace(customResource.getMetadata().getNamespace())
        .withName(customResource.getSpec().getName()).watch(new Watcher<Service>() {

          @Override
          public void eventReceived(Action action, Service resource) {
            // TODO Auto-generated method stub
            log.info("Received {} event on name={} version={} for custom resource {}", action,
                resource.getMetadata().getName(), resource.getMetadata().getResourceVersion(),
                customResource.getMetadata().getName());

            // service should only be created by the creation of the custom resource
            if (action == Action.ADDED) {
              return;
            }

            // trigger a reinstating of the resource to make sure it matches specification
            // in customresource
            OnDependentResourceChange(customResource, resource);
          }

          @Override
          public void onClose(WatcherException cause) {
            // TODO Auto-generated method stub
            log.info("Closing watch on service name={}", name);
          }
      
    });

    // record that we have the watch going
    svcWatches.put(customResource,watch);
  }

  private void removeServiceWatcher(CustomService customResource) {
    if (!svcWatches.containsKey(customResource))
    {
      log.info( "No watcher for service {}", customResource.getMetadata().getName());
      return;
    }
    
    Watch watch=svcWatches.remove(customResource);
    watch.close();
  }

  @Override
  public DeleteControl deleteResource(CustomService resource, Context<CustomService> context) {
    // You need to remove the service watcher first otherwise it will reinstate the service as you attempt to delete it
    removeServiceWatcher(resource);

    log.info("Execution deleteResource for: {}", resource.getMetadata().getName());
    kubernetesClient
        .services()
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getSpec().getName())
        .delete();

    return DeleteControl.DEFAULT_DELETE;
  }

  @Override
  public UpdateControl<CustomService> createOrUpdateResource(
      CustomService resource, Context<CustomService> context) {
    log.info("Execution createOrUpdateResource for: {}", resource.getMetadata().getName());

    // Remove any watches on resources managed by this custom resource whilst we're updating it
    removeServiceWatcher(resource);

    updateResource(resource);

    // Reinstate watches on resources that are managed by this custom resource
    setupServiceWatcher(resource);

    return UpdateControl.updateCustomResource(resource);
  }

  public void OnDependentResourceChange(CustomService managingResource, Object managedResource)
  {
    log.info("Execution OnDependentResourceChange for: {} on resource {}", managingResource.getMetadata().getName(),
      managedResource);

    // Remove any watches on resources managed by this custom resource whilst we're updating it
    removeServiceWatcher(managingResource);

    updateResource(managingResource);

    // Reinstate watches on resources that are managed by this custom resource
    setupServiceWatcher(managingResource);
  }

  private void updateResource(CustomService resource) {

    ServicePort servicePort = new ServicePort();
    servicePort.setPort(8080);
    ServiceSpec serviceSpec = new ServiceSpec();
    serviceSpec.setPorts(Collections.singletonList(servicePort));

    kubernetesClient
        .services()
        .inNamespace(resource.getMetadata().getNamespace())
        .createOrReplaceWithNew()
        .withNewMetadata()
        .withName(resource.getSpec().getName())
        .addToLabels("testLabel", resource.getSpec().getLabel())
        .endMetadata()
        .withSpec(serviceSpec)
        .done();
  
  }
}

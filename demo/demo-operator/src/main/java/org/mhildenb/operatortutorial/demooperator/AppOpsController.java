package org.mhildenb.operatortutorial.demooperator;

import org.jboss.logging.Logger;
import org.mhildenb.operatortutorial.logmodule.LogModule;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

import java.util.Optional;


import javax.inject.Inject;

/** A very simple sample controller that creates a service with a label. */
@Controller
public class AppOpsController implements ResourceController<AppOps> {

  @Inject 
  LogModule logModule; // = new LogModule();

  Logger log;

  private final KubernetesClient kubernetesClient;

  private DeploymentEventSource deploymentEventSource;

  public AppOpsController(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  @Override
  public void init(EventSourceManager eventSourceManager) 
  {
    // constructor is too early for the logger to be injected
    log = logModule.getLogger();

    this.deploymentEventSource = DeploymentEventSource.create(kubernetesClient);
    eventSourceManager.registerEventSource("deployment-event-source", this.deploymentEventSource);

  }

  @Override
  public UpdateControl<AppOps> createOrUpdateResource(AppOps resource, Context<AppOps> context) 
  {
    log.info(String.format("Execution createOrUpdateResource for: %s", resource.getMetadata().getName()));

    // register for events for any deployments associated with this AppOps
    deploymentEventSource.registerWatch(resource);

    // Optional<CustomResourceEvent> latestCREvent =
    //     context.getEvents().getLatestOfType(CustomResourceEvent.class);
    // if (latestCREvent.isPresent()) {
    //   createOrUpdateDeployment(resource);
    // }

    Optional<DeploymentEvent> latestDeploymentEvent =
        context.getEvents().getLatestOfType(DeploymentEvent.class);
    if (latestDeploymentEvent.isPresent()) 
    {
      AppOps updatedResource = updateAppOpsStatus(resource, latestDeploymentEvent.get().getDeployment());
      
      log.info(String.format(
            "Updating status of AppOps %s in namespace %s to %s",
            resource.getMetadata().getName(),
            resource.getMetadata().getNamespace(), 
            "" ));

      return UpdateControl.updateStatusSubResource(updatedResource);
    }

    return UpdateControl.noUpdate();
  }

  private AppOps updateAppOpsStatus(AppOps resource, Deployment deployment) {
    // FIXME: Update status on resource
    // DeploymentStatus deploymentStatus =
    //     Objects.requireNonNullElse(deployment.getStatus(), new DeploymentStatus());
    // int readyReplicas = Objects.requireNonNullElse(deploymentStatus.getReadyReplicas(), 0);
    // TomcatStatus status = new TomcatStatus();
    // status.setReadyReplicas(readyReplicas);
    // tomcat.setStatus(status);
    return resource;
  }

  @Override
  public DeleteControl deleteResource(AppOps resource, Context<AppOps> context) {
    log.info(String.format("Execution deleteResource for: %s", resource.getMetadata().getName()));

    // FIXME: Unregister for watch of the label for resource?

    return DeleteControl.DEFAULT_DELETE;
  }

  private void updateResource(AppOps resource) {

    // ServicePort servicePort = new ServicePort();
    // servicePort.setPort(8080);
    // ServiceSpec serviceSpec = new ServiceSpec();
    // serviceSpec.setPorts(Collections.singletonList(servicePort));

    // kubernetesClient
    //     .services()
    //     .inNamespace(resource.getMetadata().getNamespace())
    //     .createOrReplace(new ServiceBuilder().
    //       withNewMetadata().
    //       withName(resource.getSpec().getName()).
    //       addToLabels("testLabel", resource.getSpec().getLabel()).
    //       endMetadata().
    //       withSpec(serviceSpec).
    //       build()
    //     );
  
  }
}

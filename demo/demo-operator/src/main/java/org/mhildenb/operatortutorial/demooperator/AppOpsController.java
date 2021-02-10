package org.mhildenb.operatortutorial.demooperator;

import org.jboss.logging.Logger;
import org.mhildenb.operatortutorial.logmodule.LogModule;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;

import java.util.Optional;


import javax.inject.Inject;

/** A very simple sample controller that creates a service with a label. */
@Controller
public class AppOpsController implements ResourceController<AppOps> {

  @Inject 
  LogModule logModule; // = new LogModule();

  Logger log;

  private final KubernetesClient kubernetesClient;

  private PodEventSource podEventSource;

  public AppOpsController(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  @Override
  public void init(EventSourceManager eventSourceManager) 
  {
    // constructor is too early for the logger to be injected
    log = logModule.getLogger();

    this.podEventSource = PodEventSource.create(kubernetesClient);
    eventSourceManager.registerEventSource("pod-event-source", this.podEventSource);

  }

  // This method gets called with the most recent version of the AppOps resource that is responsible directly 
  // (or indirectly by way of one of its interested) requests
  @Override
  public UpdateControl<AppOps> createOrUpdateResource(AppOps resource, Context<AppOps> context) 
  {
    log.info(String.format("Execution createOrUpdateResource for: %s", resource.getMetadata().getName()));

    // Check to see if there is an update in this cycle due to AppOps resource itself
    Optional<CustomResourceEvent> latestAppOpsEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
    if (latestAppOpsEvent.isPresent())
    {
      if (latestAppOpsEvent.get().getAction() == Action.ADDED)
      {
        // register for events for any deployments associated with this AppOps
        podEventSource.registerWatch(resource);
      }

      // FIXME: Check to see if level or threshold has changed since last event and
      // if so, then get all pods with the label and call per ip
/*
List<Pod> pods =
          kubernetesClient
              .pods()
              .inNamespace(webapp.getMetadata().getNamespace())
              .withLabels(deployment.getSpec().getSelector().getMatchLabels())
              .list()
              .getItems();
      for (Pod pod : pods) {
        log.info(
            "Executing command {} in Pod {}",
            String.join(" ", command),
            pod.getMetadata().getName());
        kubernetesClient
            .pods()
            .inNamespace(deployment.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .inContainer("war-downloader")
            .writingOutput(new ByteArrayOutputStream())
            .writingError(new ByteArrayOutputStream())
            .exec(command);
*/

        // FIXME: Update status
    }

    // See if there is a deployment event in this batch
    Optional<PodEvent> latestPodEvent = context.getEvents().getLatestOfType(PodEvent.class);
    if (latestPodEvent.isPresent()) 
    {
      Action action = latestPodEvent.get().getAction();
      if( action == Action.DELETED )
      {
        return UpdateControl.noUpdate();
      }

      Pod pod = latestPodEvent.get().getPod();
      if (pod.getStatus().getPhase().equals("Running"))
      {
        log.info( String.format("Pod is running at ip address %s", pod.getStatus().getPodIP()) );

        // FIXME: Attempt to call the log status on the pod and if different than the last configured format or level, then 
        // update that pod
      }

      AppOps updatedResource = updateAppOpsStatus(resource, latestPodEvent.get().getPod());
      
      log.info(String.format(
            "Updating status of AppOps %s in namespace %s to %s",
            resource.getMetadata().getName(),
            resource.getMetadata().getNamespace(), 
            "" ));
    }

    return UpdateControl.noUpdate();
  }

  private AppOps updateAppOpsStatus(AppOps resource, Pod pod) {
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

package org.mhildenb.operatortutorial.demooperator;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.mhildenb.operatortutorial.logmodule.LogModule;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
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

  // A map of applabel to last log threshold
  private HashMap<String, String> appLogThresholds = new HashMap<String, String>();

  public AppOpsController(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  @Override
  public void init(EventSourceManager eventSourceManager) {
    // constructor is too early for the logger to be injected
    log = logModule.getLogger();

    this.podEventSource = PodEventSource.create(kubernetesClient);
    eventSourceManager.registerEventSource("pod-event-source", this.podEventSource);

  }

  // This method gets called with the most recent version of the AppOps resource
  // that is responsible directly
  // (or indirectly by way of one of its interested) requests
  @Override
  public UpdateControl<AppOps> createOrUpdateResource(AppOps resource, Context<AppOps> context) {
    log.info(String.format("Execution createOrUpdateResource for: %s", resource.getMetadata().getName()));

    // Check to see if there is an update in this cycle due to AppOps resource
    // itself
    Optional<CustomResourceEvent> latestAppOpsEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
    if (latestAppOpsEvent.isPresent()) {
      // FIXME: Get status back
      updateAppOps(resource, latestAppOpsEvent.get());
    }

    // See if there is a pod event in this batch
    Optional<PodEvent> latestPodEvent = context.getEvents().getLatestOfType(PodEvent.class);
    if (latestPodEvent.isPresent()) {
      Action action = latestPodEvent.get().getAction();
      if (action == Action.DELETED) {
        return UpdateControl.updateCustomResource(resource);
      }

      Pod pod = latestPodEvent.get().getPod();
      updateLogLevels(pod, resource.getSpec().getLogSpec().getLogThreshold());
    }

    //AppOps updatedResource = updateAppOpsStatus(resource, latestPodEvent.get().getPod());

    log.info(String.format("Updating status of AppOps %s in namespace %s to %s", resource.getMetadata().getName(),
        resource.getMetadata().getNamespace(), ""));

    return UpdateControl.updateCustomResource(resource);
  }

  private AppOps updateAppOpsStatus(AppOps resource, Pod pod) {
    // FIXME: Update status on resource
    // DeploymentStatus deploymentStatus =
    // Objects.requireNonNullElse(deployment.getStatus(), new DeploymentStatus());
    // int readyReplicas =
    // Objects.requireNonNullElse(deploymentStatus.getReadyReplicas(), 0);
    // TomcatStatus status = new TomcatStatus();
    // status.setReadyReplicas(readyReplicas);
    // tomcat.setStatus(status);
    return resource;
  }

  @Override
  public DeleteControl deleteResource(AppOps resource, Context<AppOps> context) {
    log.info(String.format("Execution deleteResource for: %s", resource.getMetadata().getName()));

    // Unregister for watches on pods matching the AppOps resource label
    podEventSource.unregisterWatch(resource);

    return DeleteControl.DEFAULT_DELETE;
  }

  private UpdateControl<AppOps> updateAppOps(AppOps resource, CustomResourceEvent latestAppOpsEvent) {
    assert (latestAppOpsEvent.getAction() != Action.DELETED);

    String newDesiredLogLevel = resource.getSpec().getLogSpec().getLogThreshold();
    String deploymentLabel = resource.getSpec().getDeploymentLabel();

    if (latestAppOpsEvent.getAction() == Action.ADDED) {
      // register for events for any deployments associated with this AppOps
      podEventSource.registerWatch(resource);

      // record the desired log level
      appLogThresholds.put(deploymentLabel, newDesiredLogLevel);

      return UpdateControl.updateCustomResource(resource);
    }

    String previousLogLevel = appLogThresholds.get(deploymentLabel);
    if (newDesiredLogLevel != previousLogLevel) {
      List<Pod> list = kubernetesClient.pods().withLabel("app", deploymentLabel).list().getItems();

      Integer numUpdated = updateLogLevels(list, newDesiredLogLevel);
    }

    // FIXME: Update status based on number of pods successfully updated
    return UpdateControl.updateCustomResource(resource);
  }

  private Integer updateLogLevels(List<Pod> list, String newThreshold) {
    Integer numPodsUpdated = 0;

    for (Pod pod : list) {
      if (updateLogLevels(pod, newThreshold)) {
        numPodsUpdated++;
      }
    }

    return numPodsUpdated;
  }

  URI getPodURI(Pod pod) throws URISyntaxException {
    return URI.create(String.format("http://%s:8080", pod.getStatus().getPodIP()));
    // return URI.create("http://localhost:8086");
  }

  // returns three if pod was updated successfully
  private Boolean updateLogLevels(Pod pod, String newThreshold) {
    String podName = pod.getMetadata().getName();

    if (pod.getStatus().getPhase().equals("Running")) {
      try {
        URI podUri = getPodURI(pod);
        Level desiredThreshold = Logger.Level.valueOf(newThreshold);

        if (logModule.getLogLevel(podUri) != desiredThreshold ) {
          logModule.changeLogLevel(podUri, desiredThreshold );

          log.info(String.format("Updated log level for pod %s", podName));
        }

        return true;
      }
      catch (Exception e) {
        log.error(String.format("Unable to update pod %s.  Error is: %s", podName, e.toString()));
      }
    }

    log.info( String.format("Skipping pod (%s) not in running state.", podName));

    return false;
  }
}

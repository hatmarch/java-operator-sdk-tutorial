package org.mhildenb.operatortutorial.demooperator;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
      return updateAppOps(resource, latestAppOpsEvent.get());
    }

    // See if there is a pod event in this batch
    Optional<PodEvent> latestPodEvent = context.getEvents().getLatestOfType(PodEvent.class);
    if (!latestPodEvent.isPresent()) {
      return UpdateControl.noUpdate();
    }


    Action action = latestPodEvent.get().getAction();
    if (action == Action.DELETED) {
      log.info(String.format("Pod %s in namespace %s is deleted", resource.getMetadata().getName(),
      resource.getMetadata().getNamespace(), resource.getStatus().getMessage()));

      // FIXME: Update status
      return UpdateControl.updateCustomResource(resource);
    }

    Pod pod = latestPodEvent.get().getPod();
    AppOpsStatus status = updateLogLevels(pod, resource.getSpec().getLogSpec().getLogThreshold());
  
    resource.setStatus(status);

    log.info(String.format("Updating status of AppOps %s in namespace %s to %s", resource.getMetadata().getName(),
        resource.getMetadata().getNamespace(), resource.getStatus().getMessage()));

    return UpdateControl.updateStatusSubResource(resource);
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
    }

    String previousLogLevel = appLogThresholds.get(deploymentLabel);
    if (newDesiredLogLevel != previousLogLevel) {
      List<Pod> list = kubernetesClient.pods().withLabel("app", deploymentLabel).list().getItems();

      AppOpsStatus status = updateLogLevels(list, newDesiredLogLevel);
      resource.setStatus(status);

      return UpdateControl.updateStatusSubResource(resource);
    }

    // If we get here then no changes this loop
    return UpdateControl.noUpdate();
  }

  private AppOpsStatus updateLogLevels(List<Pod> list, String newThreshold) {
    StringBuilder sbMsg = new StringBuilder();

    for (Pod pod : list) {
      // if we've been through more than once add a newline
      if( sbMsg.length() > 0)
      {
        sbMsg.append("%n");
      }

      // pull out the message for pod and add to the overall message
      AppOpsStatus status = updateLogLevels(pod, newThreshold);
      sbMsg.append(status.getMessage());
    }

    return AppOpsStatus.create(sbMsg.toString());
  }

  @ConfigProperty(name="demo-operator.pod-uri-override", defaultValue=" ")
  String podUriOverride;

  URI getPodURI(Pod pod) throws URISyntaxException 
  {
    if( !podUriOverride.isBlank() )
    {
      return URI.create(podUriOverride); 
    }

    return URI.create(String.format("http://%s:8080", pod.getStatus().getPodIP()));
  }

  // returns three if pod was updated successfully
  private AppOpsStatus updateLogLevels(Pod pod, String newThreshold) {
    StringBuilder sbMsg = new StringBuilder();
    String podName = pod.getMetadata().getName();

    sbMsg.append(String.format("Pod: %s> ", pod.getMetadata().getName()));

    if (pod.getStatus().getPhase().equals("Running")) {
      try {
        URI podUri = getPodURI(pod);
        Level desiredThreshold = Logger.Level.valueOf(newThreshold);

        if (logModule.getLogLevel(podUri) != desiredThreshold ) {
          logModule.changeLogLevel(podUri, desiredThreshold );

          log.info(String.format("Updated log level for pod %s", podName));
        }

        sbMsg.append(String.format("%s",newThreshold));
        
      }
      catch (Exception e) {
        log.error(String.format("Unable to update pod %s.  Error is: %s", podName, e.toString()));

        sbMsg.append(String.format("ERROR: %s",e.getMessage()));
      }
    }
    else
    {
      log.info( String.format("Skipping pod (%s) not in running state.", podName));

      sbMsg.append(String.format("Not running..."));
    }

    return AppOpsStatus.create(sbMsg.toString());
  }
}

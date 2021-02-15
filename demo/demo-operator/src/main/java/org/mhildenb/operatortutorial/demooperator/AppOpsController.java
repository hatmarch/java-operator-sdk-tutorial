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
import io.javaoperatorsdk.operator.processing.event.internal.TimerEvent;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

  private TimerEventSource timerEventSource;

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

    this.timerEventSource = new TimerEventSource();
    eventSourceManager.registerEventSource("timer-event-source", this.timerEventSource);
  }

  // This method gets called with the most recent version of the AppOps resource
  // that is responsible directly
  // (or indirectly by way of one of its interested) requests
  @Override
  public UpdateControl<AppOps> createOrUpdateResource(AppOps resource, Context<AppOps> context) {
    log.info(String.format("Execution createOrUpdateResource for: %s", resource.getMetadata().getName()));

    var updateControls = new ArrayList<UpdateControl<AppOps>>();

    // Check to see if there is an update in this cycle due to AppOps resource
    // itself
    Optional<CustomResourceEvent> latestAppOpsEvent = context.getEvents().getLatestOfType(CustomResourceEvent.class);
    if (latestAppOpsEvent.isPresent()) {
      updateControls.add(updateAppOps(resource, latestAppOpsEvent.get()));
    }

    // See if there is a timer eventin in this batch
    Optional<TimerEvent> latestTimerEvent = context.getEvents().getLatestOfType(TimerEvent.class);
    if (latestTimerEvent.isPresent()) {
      // FIXME: flush all other timer events?
      updateControls.add(updateTimer(resource, latestTimerEvent.get()));
    }

    // See if there is a pod event in this batch
    Optional<PodEvent> latestPodEvent = context.getEvents().getLatestOfType(PodEvent.class);
    if (latestPodEvent.isPresent()) {
      updateControls.add(onPodEvent(resource, latestPodEvent.get()));
    }

    return mergeUpdateControls(updateControls);
  }

  private UpdateControl<AppOps> mergeUpdateControls(ArrayList<UpdateControl<AppOps>> updates)
  {
    if( updates == null )
    {
      return UpdateControl.noUpdate();
    }

    Boolean updateCR = false;
    Boolean updateStatus = false;
    AppOps resource = null;
    for( UpdateControl<AppOps> update : updates )
    {
      updateCR = updateCR || update.isUpdateCustomResource() || update.isUpdateCustomResource();
      updateStatus = updateStatus || update.isUpdateStatusSubResource() || update.isUpdateCustomResource();
      resource = (resource == null ) ? update.getCustomResource() : resource;
    }

    if (resource == null)
    {
      return UpdateControl.noUpdate();
    }
    else if (updateCR && updateStatus)
    {
      return UpdateControl.updateCustomResourceAndStatus(resource);
    }
    else if (updateCR)
    {
      return UpdateControl.updateCustomResource(resource);
    }

    return UpdateControl.updateStatusSubResource(resource);
  }

  private UpdateControl<AppOps> onPodEvent(AppOps resource, PodEvent podEvent) 
  {
    Action action = podEvent.getAction();
    if (action == Action.DELETED) {
      log.info(String.format("Pod %s in namespace %s is deleted", resource.getMetadata().getName(),
      resource.getMetadata().getNamespace(), resource.getStatus().getMessage()));

      // FIXME: Remove pod from spec
      assert( resource.getSpec().getPodLogSpecs() != null );
      Boolean removed = resource.getSpec().getPodLogSpecs().remove(podEvent.getPod().getMetadata().getName());
      if (removed) {
        // FIXME: Update status
        return UpdateControl.updateCustomResource(resource);
      }
      else
      {
        UpdateControl.noUpdate();
      }
    }
    else if (action == Action.ADDED)
    {
      return addPodToSpec( resource, podEvent.getPod() );
    }

    // assume modified event, nothing to do right now
    return UpdateControl.noUpdate();
  }

  private UpdateControl<AppOps> addPodToSpec(AppOps resource, Pod pod) {
    List<PodLogSpec> podSpecs = resource.getSpec().getPodLogSpecs();
    if (podSpecs == null)
    {
      podSpecs = new ArrayList<PodLogSpec>();
      resource.getSpec().setPodLogSpecs(podSpecs);
    }

    PodLogSpec spec = new PodLogSpec();
    String podName = pod.getMetadata().getName();
    Optional<PodLogSpec> podLogSpec = resource.getSpec().getPodLogSpec(podName);
    if( !podLogSpec.isPresent() )
    {
      spec = PodLogSpec.createFromName(podName);
      podSpecs.add(spec);
    }
    else
    {
      spec = podLogSpec.get();
    }
  
    // FIXME: Look this up
    spec.elevatedLogging = true;

    // spec should be added and up to date

    return UpdateControl.updateCustomResource(resource);
  }

  private UpdateControl<AppOps> updateTimer(AppOps resource, TimerEvent latestTimerEvent) {
    log.info(String.format("Timer Event for: %s.  Threshold: %d", 
      resource.getMetadata().getName(), resource.getSpec().getLogSpec().getOutstandingRequestThreshold()));

    // FIXME: check to see if threshold exceeded and if so set log level on CR
    return UpdateControl.noUpdate();
  }

  @Override
  public DeleteControl deleteResource(AppOps resource, Context<AppOps> context) {
    log.info(String.format("Execution deleteResource for: %s", resource.getMetadata().getName()));

    // Unregister for watches on pods matching the AppOps resource label
    podEventSource.unregisterWatch(resource);

    return DeleteControl.DEFAULT_DELETE;
  }

  private UpdateControl<AppOps> updateAppOps(AppOps resource, CustomResourceEvent latestAppOpsEvent) {
    switch (latestAppOpsEvent.getAction()) {
      case ADDED:
        return onAppOpsAdded(resource, latestAppOpsEvent);
      case MODIFIED:
        return onAppOpsModified(resource, latestAppOpsEvent);
      case DELETED:
        assert(false); //, "Should not be getting a delete event here");
        break;
      default:
        assert(false); //, String.format("Unknown event type %s", latestAppOpsEvent.getAction()));
        break;
    }

    return UpdateControl.noUpdate();
  }

  private UpdateControl<AppOps> onAppOpsAdded(AppOps resource, CustomResourceEvent latestAppOpsEvent)
  {
    // register for events for any deployments associated with this AppOps
    podEventSource.registerWatch(resource);

    // get a callback every 2 seconds
    timerEventSource.schedule(resource, 0, 2*1000);

    // // record the desired log level
    // appLogThresholds.put(deploymentLabel, newDesiredLogLevel);

    // If we get here then no changes this loop
    return UpdateControl.noUpdate();
  }

  private UpdateControl<AppOps> onAppOpsModified(AppOps resource, CustomResourceEvent latestAppOpsEvent) 
  {
    // FIXME: Run through pods in the spec and update desired state
    List<PodLogSpec> podLogSpecs = resource.getSpec().getPodLogSpecs();
    if (podLogSpecs == null) {
      return UpdateControl.noUpdate();
    }

    AppOpsStatus status= updateLogLevels(podLogSpecs, resource.getSpec().getLogSpec().getLogThreshold());
    resource.setStatus(status);

    return UpdateControl.updateStatusSubResource(resource);
  }

  private AppOpsStatus updateLogLevels( List<PodLogSpec> podLogSpecs, String strDefaultLogLevel) 
  {
    StringBuilder sbMsg = new StringBuilder();

    for (PodLogSpec podLogSpec : podLogSpecs) {
      // if we've been through more than once add a newline
      if( sbMsg.length() > 0)
      {
        sbMsg.append("%n");
      }

      // pull out the message for pod and add to the overall message
      String strNewLevel = strDefaultLogLevel;
      if (podLogSpec.elevatedLogging)
      {
        strNewLevel = Logger.Level.TRACE.toString();
      }

      var podResource = kubernetesClient.pods().withName(podLogSpec.name);
      if( podResource.get() != null )
      {
        // pull out the message for pod and add to the overall message
        AppOpsStatus status = updateLogLevels(podResource.get(), strNewLevel );
        sbMsg.append(status.getMessage());
      }
      else
      {
        // leave this pod out of our status
        log.info(String.format("No pod with name %s", podLogSpec.name));
      }
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

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
import java.util.function.Function;
import java.util.stream.Collectors;

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
      updateControls.add(updateTimer(resource, latestTimerEvent.get()));
    }

    // See if there is a pod event in this batch
    Optional<PodEvent> latestPodEvent = context.getEvents().getLatestOfType(PodEvent.class);
    if (latestPodEvent.isPresent()) {
      updateControls.add(onPodEvent(resource, latestPodEvent.get()));
    }

    return mergeUpdateControls(updateControls);
  }

  private UpdateControl<AppOps> mergeUpdateControls(ArrayList<UpdateControl<AppOps>> updates) {
    if (updates == null) {
      return UpdateControl.noUpdate();
    }

    Boolean updateCR = false;
    Boolean updateStatus = false;
    AppOps resource = null;
    for (UpdateControl<AppOps> update : updates) {
      updateCR = updateCR || update.isUpdateCustomResource() || update.isUpdateCustomResource();
      updateStatus = updateStatus || update.isUpdateStatusSubResource() || update.isUpdateCustomResource();
      resource = (resource == null) ? update.getCustomResource() : resource;
    }

    if (resource == null) {
      return UpdateControl.noUpdate();
    } else if (updateCR && updateStatus) {
      return UpdateControl.updateCustomResourceAndStatus(resource);
    } else if (updateCR) {
      return UpdateControl.updateCustomResource(resource);
    }

    return UpdateControl.updateStatusSubResource(resource);
  }

  private UpdateControl<AppOps> onPodEvent(AppOps resource, PodEvent podEvent) 
  {
    String podName = podEvent.getPod().getMetadata().getName();

    Action action = podEvent.getAction();
    if (action == Action.DELETED) {
      log.info(String.format("Pod %s in namespace %s is deleted", resource.getMetadata().getName(),
      resource.getMetadata().getNamespace()));

      // Remove the pod from the AppOps Spec so we quit trying to update logging
      assert( resource.getSpec().getPodLogSpecs() != null );
      var removedSpec = resource.getSpec().removePodLogSpec(podName);
      if (removedSpec.isPresent()) {
        log.info(String.format("Removed pod %s from AppOps spec", podName ));
      }
      else
      {
        log.trace(String.format("Could find removed pod %s in AppOps spec", podName ));
      }

      // FIXME: We can lose pod deleted events if multiple pod delete events come in
      // on the same update frame (or if they just get lost)
    }

    // NOTE: Since we might miss pod events (multiple events in one frame for a pod) just recreate the 
    // spec for any pods we don't have
    var podList = kubernetesClient.pods().withLabel("app", resource.getSpec().getDeploymentLabel()).list().getItems();
    for( var curPod : podList )
    {
      String curPodName = curPod.getMetadata().getName();
      if (!resource.isInPodSpec(curPodName)) 
      {
        log.info(String.format("Found a pod (%s) that was previously not in CR list", curPodName));

        addPodToSpec(resource, curPod);
      }
    }

    var names = podList.stream().map( n -> n.getMetadata().getName() ).collect(Collectors.toList());
    // Make sure the podspecs only match those in the podList (which represents the most current state)
    resource.reconcilePodLogSpecs(names);

    // try to get a chance registered on the AppOps resource
    return UpdateControl.updateCustomResource(resource);
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
  
    // Determine whether this pod should have elevated logging
    // and write it back on the resource
    evalElevatedLogging(spec, resource, pod);

    // spec should be added and up to date.
    // We will act on what we've written on the customresource when we get a callback on its changes

    return UpdateControl.updateCustomResource(resource);
  }

  private UpdateControl<AppOps> updateTimer(AppOps resource, TimerEvent latestTimerEvent) {
    log.trace(String.format("Timer Event for: %s.  Threshold: %d", 
      resource.getMetadata().getName(), resource.getSpec().getLogSpec().getOutstandingRequestThreshold()));

    // Look through all pods currently on the resource and update thresholds
    for( PodLogSpec spec : resource.getSpec().getPodLogSpecs() )
    {
      evalElevatedLogging(spec, resource);
    }

    // if there are updates still pending, then attempt to update log levels now
    if( resource.getStatus().pending)
    {
      log.info("Found pending status in timer event");

      var podLogSpecs = resource.getSpec().getPodLogSpecs();

      // This will set the status to pending if there are still pods to be updated
      AppOpsStatus status= updateLogLevels(podLogSpecs, resource.getSpec().getLogSpec().getLogThreshold());
      resource.setStatus(status);
    }

    return UpdateControl.updateCustomResourceAndStatus(resource);
  }

  // determines wheter the pod in the spec meets critera for elevated logging and writes this
  // back on the spec
  private void evalElevatedLogging(PodLogSpec spec, AppOps resource ) 
  {
    var pod = kubernetesClient.pods().withName(spec.name);
    if( pod.get() != null )
    {
      evalElevatedLogging(spec, resource, pod.get());
    }

    // otherwise, assume default
  }

  private void evalElevatedLogging(PodLogSpec spec, AppOps resource, Pod pod)
  {
    int pendingRequests = 0;
    try
    {
      pendingRequests = logModule.getPendingRequests(getPodURI(pod));
    }
    catch( Exception e )
    {
      log.info(String.format("Unable to contact pod %s to determine pending requests", pod.getMetadata().getName()));
    }
    spec.elevatedLogging = (pendingRequests > resource.getOutstandingRequestThreshold());
  }

  @Override
  public DeleteControl deleteResource(AppOps resource, Context<AppOps> context) {
    log.info(String.format("Execution deleteResource for: %s", resource.getMetadata().getName()));

    // Unregister for watches on pods matching the AppOps resource label
    podEventSource.unregisterWatch(resource);

    // FIXME: Not sure this is strictly necessary...sdk may clean out this timer on delete
    timerEventSource.cancelSchedule(resource.getMetadata().getUid());

    return DeleteControl.DEFAULT_DELETE;
  }

  private UpdateControl<AppOps> updateAppOps(AppOps resource, CustomResourceEvent latestAppOpsEvent) {
    switch (latestAppOpsEvent.getAction()) {
      case ADDED:
        return onAppOpsAdded(resource, latestAppOpsEvent);
      case MODIFIED:
        return onAppOpsModified(resource, latestAppOpsEvent);
      case DELETED:
        // SDK calls deleteResource which has a different return value instead
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

    // reset pod list and status
    resource.getSpec().setPodLogSpecs(new ArrayList<PodLogSpec>());
    resource.setStatus(AppOpsStatus.create());

    // If we get here then no changes this loop
    return UpdateControl.updateCustomResourceAndStatus(resource);
  }

  private UpdateControl<AppOps> onAppOpsModified(AppOps resource, CustomResourceEvent latestAppOpsEvent) 
  {
    var podLogSpecs = resource.getSpec().getPodLogSpecs();

    // This will set the status to pending if there are still pods to be updated
    AppOpsStatus status= updateLogLevels(podLogSpecs, resource.getSpec().getLogSpec().getLogThreshold());
    resource.setStatus(status);

    return UpdateControl.updateCustomResourceAndStatus(resource);
  }

  private AppOpsStatus updateLogLevels( List<PodLogSpec> podLogSpecs, String strDefaultLogLevel) 
  {
    AppOpsStatus status = AppOpsStatus.create();
    status.pending = true;
    status.podLogStatuses = new ArrayList<PodLogStatus>();

    Boolean anyPending = false;
    for (PodLogSpec podLogSpec : podLogSpecs) {

      // pull out the message for pod and add to the overall message
      String strNewLevel = strDefaultLogLevel;
      if (podLogSpec.elevatedLogging)
      {
        strNewLevel = Logger.Level.TRACE.toString();
      }

      var podResource = kubernetesClient.pods().withName(podLogSpec.name);
      if( podResource.get() != null )
      {
        // merge in any podLogStatus from updateLogLevels (should only be one)
        AppOpsStatus podUpdateStatus = updateLogLevels(podResource.get(), strNewLevel );
        status.podLogStatuses.addAll(podUpdateStatus.podLogStatuses);

        // If this status returns pending, then there is a pod that couldn't be accessed
        anyPending = ( anyPending || podUpdateStatus.pending );
      }
      else
      {
        // leave this pod out of our status
        log.info(String.format("No pod with name %s", podLogSpec.name));
      }
    }

    status.pending = anyPending;

    return status;
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
  private AppOpsStatus updateLogLevels(Pod pod, String newThreshold) 
  {
    String podName = pod.getMetadata().getName();

    AppOpsStatus status = AppOpsStatus.create();
    status.pending = true;

    status.podLogStatuses = new ArrayList<PodLogStatus>();

    var podStatus = PodLogStatus.create(pod.getMetadata().getName());
    status.podLogStatuses.add(podStatus);

    if (pod.getStatus().getPhase().equals("Running")) {
      try {
        URI podUri = getPodURI(pod);
        Level desiredThreshold = Logger.Level.valueOf(newThreshold);

        if (logModule.getLogLevel(podUri) != desiredThreshold ) {
          logModule.changeLogLevel(podUri, desiredThreshold );

          log.info(String.format("Updated log level for pod %s", podName));
        }

        // if here, then pod log levels successfully updated
        podStatus.currentLogThreshold = desiredThreshold.toString();
        status.pending = false;
      }
      catch (Exception e) {
        log.error(String.format("Unable to update pod %s.  Error is: %s", podName, e.toString()));

        podStatus.message = String.format("ERROR: %s",e.getMessage());
      }
    }
    else
    {
      log.info( String.format("Skipping pod (%s) not in running state.", podName));

      podStatus.message = "Not running...";
    }

    // truncate message
    if( podStatus.message != null && podStatus.message.length() > 128)
    {
      podStatus.message = String.format("%s...", podStatus.message.substring(0,125));
    }

    return status;
  }
}

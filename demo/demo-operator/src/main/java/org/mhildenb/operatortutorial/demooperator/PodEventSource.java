package org.mhildenb.operatortutorial.demooperator;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

import java.util.HashMap;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.quarkus.runtime.StartupEvent;

import org.jboss.logging.Logger;
import org.mhildenb.operatortutorial.logmodule.LogModule;

public class PodEventSource extends AbstractEventSource
{
  // Factory 
  public static PodEventSource create(KubernetesClient client) {
    PodEventSource podEventSource = new PodEventSource(client);
    return podEventSource;
  }
  
  private HashMap<String, Watch> podWatches=new HashMap<String, Watch>();

  void onStart(@Observes StartupEvent ev) 
  {
    // FIXME: See here: https://www.vogella.com/tutorials/DependencyInjection/article.html
    // can't have static methods
    log = logModule.getLogger();
  }

  private PodEventSource(KubernetesClient client) {
    this.client = client;
  }

  // Attempt to register a watch appropriate for Deployments that are interesting to governingResource
  // If a watch is already registered, this returns true
  public Boolean registerWatch(AppOps governingResource) {
    String deploymentLabel = governingResource.getSpec().getDeploymentLabel();

    if (podWatches.containsKey(deploymentLabel))
    {
      log.info(String.format("Already registered watch for %s", deploymentLabel));
      return false;
    }

    var podWatch = client
        .pods()
        .inNamespace(governingResource.getMetadata().getNamespace())
        .withLabel("app", deploymentLabel)
        // use nested watcher to capture the governingResource in the closure
        .watch(new Watcher<Pod>() {

          @Override
          public void eventReceived(Action action, Pod resource) {
            podEventReceived(governingResource, action, resource);
            
          }

          @Override
          public void onClose(WatcherException cause) {
            // use the governingResource to help handle the closeout
            podEventClosed(governingResource, cause);
          }

        });
        
    podWatches.put(deploymentLabel, podWatch);

    return true;
  }

  public void unregisterWatch(AppOps resource) 
  {
    String deploymentLabel = resource.getSpec().getDeploymentLabel();
    var watch = podWatches.remove(deploymentLabel);
    if (watch != null)
    {
      log.info(String.format("Closing watch for label: %s", deploymentLabel));
      watch.close();
    }
  }

  private void podEventReceived(AppOps governingResource, Action action, Pod pod) {
    log.info(String.format(
          "Event received for action: %s, pod: %s (status=%s)",
          action.name(),
          pod.getMetadata().getName(),
          pod.getStatus().getPhase()));

    if (action == Action.ERROR) {
      log.warn(String.format(
            "Skipping %s event for custom resource uid: %s, version: %s",
            action,
            getUID(pod),
            getVersion(pod)));
      return;
    }

    // This will thread events through the updateOrCreate call for the controller of the owningResource
    // AppOpsController.  Doing it this way allows a stream of events relevant to AppOps controller be processed serially
    eventHandler.handleEvent(new PodEvent(governingResource, action, pod, this));
  }

  private void podEventClosed(AppOps governingResource, WatcherException e) {
    if (e == null) {
      // stop tracking this watch
      podWatches.remove(governingResource.getSpec().getDeploymentLabel());
      return;
    }

    if (e.isHttpGone()) {
      log.warn(String.format("Received error for watch(%s), will try to reconnect.", e.getMessage()));

      registerWatch(governingResource);
    } else {
      // Note that this should not happen normally, since fabric8 client handles reconnect.
      // In case it tries to reconnect this method is not called.
      log.error(String.format("Unexpected error happened with watch. Will exit.  Error was: %s", e.getMessage()));
      System.exit(1);
    }
  }

  @Inject
  private static LogModule logModule;

  private static Logger log;

  private final KubernetesClient client;
}

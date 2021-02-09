package org.mhildenb.operatortutorial.demooperator;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.quarkus.runtime.StartupEvent;

import org.jboss.logging.Logger;
import org.mhildenb.operatortutorial.logmodule.LogModule;

public class DeploymentEventSource extends AbstractEventSource
{
  // Factory 
  public static DeploymentEventSource create(KubernetesClient client) {
    DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client);
    return deploymentEventSource;
  }
  
  void onStart(@Observes StartupEvent ev) 
  {
    // FIXME: See here: https://www.vogella.com/tutorials/DependencyInjection/article.html
    // can't have static methods
    log = logModule.getLogger();
  }

  private DeploymentEventSource(KubernetesClient client) {
    this.client = client;
  }

  public void registerWatch(AppOps governingResource) {
    client
        .apps()
        .deployments()
        .inNamespace(governingResource.getMetadata().getNamespace())
        .withLabel("app", governingResource.getSpec().getDeploymentLabel())
        // use nested watcher to capture the governingResource in the closure
        .watch(new Watcher<Deployment>() {

          @Override
          public void eventReceived(Action action, Deployment resource) {
            doEventReceived(governingResource, action, resource);
            
          }

          @Override
          public void onClose(WatcherException cause) {
            // TODO Auto-generated method stub
            doOnClose(cause);
          }

        });
  }

  private void doEventReceived(AppOps owningResource, Action action, Deployment deployment) {
    log.info(String.format(
          "Event received for action: %s, Deployment: %s (rr=%d)",
          action.name(),
          deployment.getMetadata().getName(),
          deployment.getStatus().getReadyReplicas()));

    if (action == Action.ERROR) {
      log.warn(String.format(
            "Skipping %s event for custom resource uid: %s, version: %s",
            action,
            getUID(deployment),
            getVersion(deployment)));
      return;
    }

    eventHandler.handleEvent(new DeploymentEvent(owningResource, action, deployment, this));
  }

  private void doOnClose(WatcherException e) {
    if (e == null) {
      return;
    }
    if (e.isHttpGone()) {
      log.warn("Received error for watch, will try to reconnect.", e);
      //FIXME:
      //registerWatch();
    } else {
      // Note that this should not happen normally, since fabric8 client handles reconnect.
      // In case it tries to reconnect this method is not called.
      log.error("Unexpected error happened with watch. Will exit.", e);
      System.exit(1);
    }
  }

  @Inject
  private static LogModule logModule;

  private static Logger log;

  private final KubernetesClient client;
}

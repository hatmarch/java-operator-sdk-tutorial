package org.mhildenb.operatortutorial.demooperator;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;

import org.jboss.logging.Logger;
import org.mhildenb.operatortutorial.logmodule.LogModule;

public class DeploymentEventSource extends AbstractEventSource implements Watcher<Deployment> 
{
  // FIXME: see reference here> https://github.com/java-operator-sdk/samples.git
  // See here: https://www.vogella.com/tutorials/DependencyInjection/article.html
  // can't have static methods
  @Inject
  private static LogModule logModule;

  private Logger log;

  private final KubernetesClient client;

  // Factory 
  public static DeploymentEventSource createAndRegisterWatch(KubernetesClient client, AppOps resource) {
    DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client);
    deploymentEventSource.registerWatch(resource);
    return deploymentEventSource;
  }

  private DeploymentEventSource(KubernetesClient client) {
    this.client = client;
    log = logModule.getLogger();
  }

  private void registerWatch(AppOps resource) {
    client
        .apps()
        .deployments()
        .inNamespace(resource.getMetadata().getNamespace())
        .withLabel("app", resource.getSpec().getDeploymentLabel())
        .watch(this);
  }

  @Override
  public void eventReceived(Action action, Deployment deployment) {
    log.info(String.format(
          "Event received for action: %s, Deployment: %s (rr=%i)",
          action.name(),
          deployment.getMetadata().getName(),
          deployment.getStatus().getReadyReplicas()));

    if (action == Action.ERROR) {
      log.warn(String.format(
            "Skipping {} event for custom resource uid: %s, version: %s",
            action,
            getUID(deployment),
            getVersion(deployment)));
      return;
    }

    eventHandler.handleEvent(new DeploymentEvent(action, deployment, this));
  }

  @Override
  public void onClose(WatcherException e) {
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
}

package org.mhildenb.operatortutorial.demooperator;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;

public class DeploymentEvent extends AbstractEvent {

  private final Watcher.Action action;
  private final Deployment deployment;
  private final AppOps owningResource;

  public DeploymentEvent(
      AppOps owningResource, Watcher.Action action, Deployment resource, DeploymentEventSource deploymentEventSource) {
    // TODO: this mapping is really critical and should be made more explicit
    super(owningResource.getMetadata().getUid(), deploymentEventSource);
    this.owningResource = owningResource;
    this.action = action;
    this.deployment = resource;
  }

  public Watcher.Action getAction() {
    return action;
  }

  public String resourceUid() {
    return getDeployment().getMetadata().getUid();
  }

  public AppOps getOwningResource() {
    return owningResource;
  }

  public Deployment getDeployment() {
    return deployment;
  }

  @Override
  public String toString() {
    return "CustomResourceEvent{"
        + "action="
        + action
        + ", resource=[ name="
        + getDeployment().getMetadata().getName()
        + ", kind="
        + getDeployment().getKind()
        + ", apiVersion="
        + getDeployment().getApiVersion()
        + " ,resourceVersion="
        + getDeployment().getMetadata().getResourceVersion()
        + ", markedForDeletion: "
        + (getDeployment().getMetadata().getDeletionTimestamp() != null
            && !getDeployment().getMetadata().getDeletionTimestamp().isEmpty())
        + " ]"
        + '}';
  }
}

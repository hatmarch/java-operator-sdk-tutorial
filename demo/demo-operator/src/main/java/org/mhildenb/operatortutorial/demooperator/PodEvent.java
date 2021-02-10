package org.mhildenb.operatortutorial.demooperator;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;

public class PodEvent extends AbstractEvent {

  private final Watcher.Action action;
  private final Pod pod;
  private final AppOps owningResource;

  public PodEvent(
      AppOps owningResource, Watcher.Action action, Pod resource, PodEventSource podEventSource) {
    // TODO: this mapping is really critical and should be made more explicit
    super(owningResource.getMetadata().getUid(), podEventSource);
    this.owningResource = owningResource;
    this.action = action;
    this.pod = resource;
  }

  public Watcher.Action getAction() {
    return action;
  }

  public String resourceUid() {
    return getPod().getMetadata().getUid();
  }

  public AppOps getOwningResource() {
    return owningResource;
  }

  public Pod getPod() {
    return pod;
  }

  @Override
  public String toString() {
    return "CustomResourceEvent{"
        + "action="
        + action
        + ", resource=[ name="
        + getPod().getMetadata().getName()
        + ", kind="
        + getPod().getKind()
        + ", apiVersion="
        + getPod().getApiVersion()
        + " ,resourceVersion="
        + getPod().getMetadata().getResourceVersion()
        + ", markedForDeletion: "
        + (getPod().getMetadata().getDeletionTimestamp() != null
            && !getPod().getMetadata().getDeletionTimestamp().isEmpty())
        + " ]"
        + '}';
  }
}

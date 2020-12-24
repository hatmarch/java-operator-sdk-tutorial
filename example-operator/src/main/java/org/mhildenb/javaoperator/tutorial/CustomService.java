package org.mhildenb.javaoperator.tutorial;

import io.fabric8.kubernetes.client.CustomResource;

public class CustomService extends CustomResource {

  private ServiceSpec spec;

  public ServiceSpec getSpec() {
    return spec;
  }

  public void setSpec(ServiceSpec spec) {
    this.spec = spec;
  }

  //Depends only on the name of the customservice
  @Override
  public int hashCode() {
      return getMetadata().getName().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    // for our purposes, if the customservice shares a name, consider them equal
    return ( ((CustomService) o).getMetadata().getName().equals(this.getMetadata().getName()) );
  }
}

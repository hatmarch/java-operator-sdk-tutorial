package org.mhildenb.operatortutorial.demooperator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.GetListFromLoadable;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("org.mhildenb.operatortutorial")
@Version("v1beta2")
public class AppOps extends CustomResource<AppOpsSpec,AppOpsStatus> implements Namespaced {

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
    return ( ((AppOps) o).getMetadata().getName().equals(this.getMetadata().getName()) );
  }

  // Convenience function to get outstanding request threshold
  public int getOutstandingRequestThreshold()
  {
    var spec = getSpec();
    if( spec == null )
    {
      return 0;
    }

    var logs = spec.getLogSpec();
    if( logs == null )
    {
      return 0;
    }

    return logs.getOutstandingRequestThreshold();
  }

  public boolean isInPodSpec(String podName) 
  {
    var spec = getSpec();
    if( spec == null )
    {
      return false;
    }

    if( spec.getPodLogSpec(podName).isPresent() )
    {
      return true;
    }

    return false;
  }
}

package org.mhildenb.operatortutorial.demooperator;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppOpsSpec {

  private String deploymentLabel;
  
  @JsonProperty("logging")
  private LogSpec logging;

  @JsonProperty("pods")
  private List<PodLogSpec> podLogLevels;

  public LogSpec getLogSpec() {
    return logging;
  }

  public void setLogSpec(LogSpec logSpec) {
    this.logging = logSpec;
  }

  public List<PodLogSpec> getPodLogSpecs() {
    return podLogLevels;
  }

  public void setPodLogSpecs(List<PodLogSpec> podLogLevels) {
    this.podLogLevels = podLogLevels;
  }

  public String getDeploymentLabel() {
    return deploymentLabel;
  }

  public void setDeploymentLabel(String deploymentLabel) {
    this.deploymentLabel = deploymentLabel;
  }

  public Optional<PodLogSpec> getPodLogSpec(String podName)
  {
    if( this.podLogLevels == null )
    {
      return Optional.empty();
    }

    int index = podLogLevels.indexOf(PodLogSpec.createFromName(podName));
    if (index < 0)
    {
      return Optional.empty();
    }
    
    return Optional.of(podLogLevels.get(index));
  }

  public Optional<PodLogSpec> removePodLogSpec( String podName )
  {
    if( this.podLogLevels == null )
    {
      return Optional.empty();
    }

    int index = podLogLevels.indexOf(PodLogSpec.createFromName(podName));
    if (index < 0)
    {
      return Optional.empty();
    }
    
    return Optional.of(podLogLevels.remove(index));
  }
}

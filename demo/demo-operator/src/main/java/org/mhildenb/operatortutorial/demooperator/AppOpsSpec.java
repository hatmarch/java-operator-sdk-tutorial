package org.mhildenb.operatortutorial.demooperator;

import java.util.List;

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
}

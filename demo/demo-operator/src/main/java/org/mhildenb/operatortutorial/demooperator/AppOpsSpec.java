package org.mhildenb.operatortutorial.demooperator;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppOpsSpec {

  private String deploymentLabel;
  
  @JsonProperty("logging")
  private LogSpec logging;

  public LogSpec getLogSpec() {
    return logging;
  }

  public void setLogSpec(LogSpec logSpec) {
    this.logging = logSpec;
  }

  public String getDeploymentLabel() {
    return deploymentLabel;
  }

  public void setDeploymentLabel(String deploymentLabel) {
    this.deploymentLabel = deploymentLabel;
  }
}

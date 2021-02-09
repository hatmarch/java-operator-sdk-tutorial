package org.mhildenb.operatortutorial.demooperator;

public class AppOpsSpec {

  private String deploymentLabel;
  private LogSpec logSpec;

  public LogSpec getLogSpec() {
    return logSpec;
  }

  public void setLogSpec(LogSpec logSpec) {
    this.logSpec = logSpec;
  }

  public String getDeploymentLabel() {
    return deploymentLabel;
  }

  public void setDeploymentLabel(String deploymentLabel) {
    this.deploymentLabel = deploymentLabel;
  }
}

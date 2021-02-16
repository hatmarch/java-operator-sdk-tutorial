package org.mhildenb.operatortutorial.demooperator;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PodLogStatus {

    public static PodLogStatus create( String podName ) {
        var podLogStatus = new PodLogStatus();
        podLogStatus.podName = podName;
        return podLogStatus;
    }

    @JsonProperty("pod")
    public String podName;

    public String currentLogThreshold;
    public String message;
}

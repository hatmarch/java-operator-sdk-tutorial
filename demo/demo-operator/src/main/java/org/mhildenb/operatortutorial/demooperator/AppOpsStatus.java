package org.mhildenb.operatortutorial.demooperator;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppOpsStatus {

    public static AppOpsStatus create()
    {
        AppOpsStatus status = new AppOpsStatus();
        status.pending = false;
        return status;
    }

    @JsonProperty("pods")
    public List<PodLogStatus> podLogStatuses;

    public Boolean pending;
}

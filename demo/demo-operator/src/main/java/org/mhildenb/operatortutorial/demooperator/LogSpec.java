package org.mhildenb.operatortutorial.demooperator;

public class LogSpec {

    private String defaultLogLevel;
	private Integer outstandingRequestThreshold;

    public String getDefaultLogLevel() {
        return defaultLogLevel;
    }

    public void setDefaultLogLevel(String logThreshold)
    {
        this.defaultLogLevel = logThreshold;
    }

    public void setOutstandingRequestThreshold(Integer outstandingRequestThreshold) {
        this.outstandingRequestThreshold = outstandingRequestThreshold;
    }
    
    public Integer getOutstandingRequestThreshold() {
        return outstandingRequestThreshold;
    }
}

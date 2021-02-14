package org.mhildenb.operatortutorial.demooperator;

public class LogSpec {
    private String logThreshold;
    // private String logFormat;
	private Integer outstandingRequestThreshold;

    public String getLogThreshold() {
        return logThreshold;
    }

    public void setLogThreshold(String logThreshold)
    {
        this.logThreshold = logThreshold;
    }

    // public String getLogFormat( )
    // {
    //     return logFormat;
    // }

    // public void setLogFormat(String logFormat) {
    //     this.logFormat = logFormat;
    // }

    public void setOutstandingRequestThreshold(Integer outstandingRequestThreshold) {
        this.outstandingRequestThreshold = outstandingRequestThreshold;
    }
    
    public Integer getOutstandingRequestTheshold() {
        return outstandingRequestThreshold;
    }
}

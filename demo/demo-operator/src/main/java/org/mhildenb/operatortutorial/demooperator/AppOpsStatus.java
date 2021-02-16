package org.mhildenb.operatortutorial.demooperator;

public class AppOpsStatus {

    public static AppOpsStatus create(String message)
    {
        AppOpsStatus status = new AppOpsStatus();
        status.message = message;
        return status;
    }

    private String message;
    public Boolean pending;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

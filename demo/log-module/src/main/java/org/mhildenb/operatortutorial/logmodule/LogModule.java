package org.mhildenb.operatortutorial.logmodule;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.Logger;

public class LogModule {

    @ConfigProperty(name = "log-module.logger-name")
    String loggerName;

    @ConfigProperty(name = "quarkus.log.level")
    String defaultLogLevel;

    public String getDefaultLogLevel() {
        return defaultLogLevel;
    }

    public Logger getLogger() {
        //assert loggerName 
        return Logger.getLogger(loggerName);
    }
}
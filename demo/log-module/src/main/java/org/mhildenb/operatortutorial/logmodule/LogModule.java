package org.mhildenb.operatortutorial.logmodule;

import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Singleton
public class LogModule {

    @ConfigProperty(name = "log-module.logger-name", defaultValue = "demo-log")
    String loggerName;

    @ConfigProperty(name = "quarkus.log.level")
    String defaultLogLevel;

    public String getDefaultLogLevel() {
        return defaultLogLevel;
    }

    public Logger getLogger() {
        assert !loggerName.isEmpty() : "loggerName has not been initialized.";

        return Logger.getLogger(loggerName);
    }
}
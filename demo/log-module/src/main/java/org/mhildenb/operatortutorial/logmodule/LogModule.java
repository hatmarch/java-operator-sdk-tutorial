package org.mhildenb.operatortutorial.logmodule;

import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@Unremovable
@Singleton
public class LogModule {

    @ConfigProperty(name = "log-module.logger-name", defaultValue = "demo-log")
    String loggerName;

    public Logger getLogger() {
        assert !loggerName.isEmpty() : "loggerName has not been initialized.";

        return Logger.getLogger(loggerName);
    }

    @ConfigProperty(name = "quarkus.log.level", defaultValue = "INFO")
    Logger.Level initialLogLevel;

    @ConfigProperty(name = "quarkus.log.console.format")
    String initialLogFormat;

    public Logger.Level getInitialLogLevel() {
        return initialLogLevel;
    }

    public String getInitialLogFormat() {
        return initialLogFormat;
    }
}
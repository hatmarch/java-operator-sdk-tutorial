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

    public void changeLogLevel(String host, Logger.Level newLevel)
    {
        // want to call the host's logging api
        // curl -X POST "http://127.0.0.1:8080/q/loggers" -H "accept: */*" -H "Content-Type: application/json" -d "{\"name\":\"org.quarkus.operatortutorial.demoapp.Main\",\"configuredLevel\":\"ERROR\"}"
    }
}
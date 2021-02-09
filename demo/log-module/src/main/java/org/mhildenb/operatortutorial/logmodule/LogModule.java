package org.mhildenb.operatortutorial.logmodule;

import java.net.URI;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
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


    // give the URI of some host, change the runtime log level of that host to newLevel
    public void changeLogLevel(URI host, Logger.Level newLevel) throws Exception
    {
        // want to call the host's logging api
        // curl -X POST "http://127.0.0.1:8080/q/loggers" -H "accept: */*" -H "Content-Type: application/json" -d "{\"name\":\"org.quarkus.operatortutorial.demoapp.Main\",\"configuredLevel\":\"ERROR\"}"

        LogModel m = new LogModel();
        m.name = loggerName;
        m.configuredLevel = newLevel.toString();

        LoggerClient logClient = RestClientBuilder.newBuilder()
            .baseUri(host)
            .build(LoggerClient.class);
        Response r = logClient.updateLogger(m);
        if (r.getStatus() < 200 && r.getStatus() >= 300)
        {
            throw new Exception(String.format("Error making call: %s", r.toString()));
        }

    }
}
package org.quarkus.operatortutorial.demoapp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@Path("/hello")
public class HelloResource {

    // FIXME: This gets injected too late
    @ConfigProperty(name = "demo-app.logger-name")
    String loggerName;

    // driven by config property so can't inject
    Logger log;

    public HelloResource()
    {
        log = Logger.getLogger(ConfigProvider.getConfig().getValue("demo-app.logger-name", String.class));
    }

    // @LoggerName(loggerName)
    // Logger log;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        log.info("Hello");
        return "hello";
    }
}

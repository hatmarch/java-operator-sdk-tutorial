package org.mhildenb.operatortutorial.demoapp;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.mhildenb.operatortutorial.logmodule.LogModule;


@Path("/hello")
public class HelloResource {

    // driven by config property so can't inject
    @Inject
    LogModule logger;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        logger.getLogger().info("Hello");
        return "hello";
    }
}

package org.mhildenb.operatortutorial.logmodule;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/q")
@Produces("application/json")
@RegisterRestClient
public interface LoggerClient {

    @POST
    @Path("/loggers")
    Response updateLogger(LogModel m);

    @GET
    @Path("/loggers")
    Response getLogger( @QueryParam("loggerName") String loggerName );
}

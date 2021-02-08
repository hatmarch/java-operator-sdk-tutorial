package org.mhildenb.operatortutorial.logmodule;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/q")
@Produces("application/json")
@RegisterRestClient
public interface LoggerClient {

    // FIXME: This is probably doomed since the URL can't be dynamic
    @POST
    @Path("/loggers")
    Response updateLogger(LogModel m);
}

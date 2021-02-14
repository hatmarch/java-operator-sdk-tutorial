package org.mhildenb.operatortutorial.demoapp;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.util.concurrent.RateLimiter;

import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.mhildenb.operatortutorial.logmodule.LogModule;


@Path("/hello")
public class HelloResource {

    // driven by config property so can't inject
    @Inject
    LogModule logger;

    // Allowable requests per second
    RateLimiter rateLimiter = RateLimiter.create(2.0); // rate is "2 permits per second"

    static AtomicInteger outstandingRequests = new AtomicInteger();

    @Gauge(name = "gaugePendingHellos",
        description = "How many hello requests are pending",
        unit = "correctness")
    public int pendingHellos() {
        //return 1;
        return outstandingRequests.get();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    // @Timed(name="timeHello",
    //         description="How long (in milliseconds) it takes to invoke hello endpoint",
    //         unit=MetricUnits.MILLISECONDS)
    public String hello() {
        int requestNum = 0;
        try {
            requestNum = outstandingRequests.incrementAndGet();

            var log = logger.getLogger();

            log.info(String.format("Checking rate limiting [%d]", requestNum));
            // simulate rate limiting
            rateLimiter.acquire();
            log.info(String.format("Acquired rate limit lock for [%d]", requestNum));
        }
        catch (Exception e)
        {

        }
        finally{
            logger.getLogger().info(String.format("Decrementing [%d]", requestNum));
            outstandingRequests.decrementAndGet();
        }
    
        return "hello";
    }
}

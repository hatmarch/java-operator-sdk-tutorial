package org.mhildenb.operatortutorial.demoapp;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Liveness
@ApplicationScoped  
public class AppHealthCheck implements HealthCheck {

    @Inject Main main;

    @Override
    public HealthCheckResponse call() {
        if (main != null && main.isRunning() )
        {
            return HealthCheckResponse.up(String.format("Up.  App: %s", main));
        }

        return HealthCheckResponse.down("App is not running");
    }
}


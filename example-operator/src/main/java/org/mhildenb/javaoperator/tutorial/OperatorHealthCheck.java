package org.mhildenb.javaoperator.tutorial;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Liveness
@ApplicationScoped  
public class OperatorHealthCheck implements HealthCheck {

    @Inject QuarkusOperator operator;

    @Override
    public HealthCheckResponse call() {
        if (operator != null && operator.isRunning() )
        {
            return HealthCheckResponse.up(String.format("Up.  Operator: %s",operator));
        }

        return HealthCheckResponse.down("Operator is not running");
    }
}


package org.mhildenb.operatortutorial.logmodule;

import io.quarkus.test.junit.QuarkusTest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

@QuarkusTest
public class LogModuleTest {

    @Inject
    LogModule logModule;

    @ConfigProperty(name = "log-module.test.integration-uri", defaultValue = "http://localhost:8084")
    String integrationHost;

    @Test
    public void testInitialLogFormat() {
        assertTrue(logModule.getInitialLogFormat().equals("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n"), 
            String.format("Unexpected initial format.  Got %s", logModule.getInitialLogFormat()));
    }

    @Test
    public void testInitialLogLevel() {
        assertTrue(logModule.getInitialLogLevel() == Logger.Level.INFO, 
            String.format("Unexpected initial log level.  Got %s", logModule.getInitialLogLevel()));
    }

    // This is more of an integration test and should 
    // only be activated when the integrationHost is available
    @Test
    @EnabledIfSystemProperty( named = "integrationTest", matches = "true" )
    @Order(1)
    public void testChangeLogLevel() {
        try{
            logModule.changeLogLevel(new URI(integrationHost), Logger.Level.FATAL);
        }
        catch ( Exception e)
        {
            assertTrue( false, String.format("Got error: %s", e.getMessage()) );
        }
    }

    @Test
    @EnabledIfSystemProperty( named = "integrationTest", matches = "true" )
    @Order(2)
    public void testGetLogLevel() {
        try{
            var logLevel = logModule.getLogLevel(new URI(integrationHost));
            assertTrue( logLevel == Logger.Level.FATAL, 
                String.format( "LogLevel did not match.  Expected 'FATAL' but received %s", logLevel ) );
        }
        catch ( Exception e)
        {
            assertTrue( false, String.format("Got error: %s", e.getMessage()) );
        }
    }
    
    @Test
    @EnabledIfSystemProperty( named = "integrationTest", matches = "true" )
    @Order(3)
    public void testPendingRequests() {
        try
        {
            int requests = logModule.getPendingRequests(URI.create(integrationHost));
            assertTrue(requests == 0, 
                String.format("Expected 0 pending requests, got %d", requests));
        }
        catch ( Exception e)
        {
            assertTrue( false, String.format("Got error: %s", e.getMessage()) );
        }
    }
}
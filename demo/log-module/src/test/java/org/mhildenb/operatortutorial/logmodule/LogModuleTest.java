package org.mhildenb.operatortutorial.logmodule;

import io.quarkus.test.junit.QuarkusTest;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

@QuarkusTest
public class LogModuleTest {

    @Inject
    LogModule logModule;

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
    // only be activated when the host is available
    //@Test
    public void testApiCall() {
        try{
            logModule.changeLogLevel(new URI("http://localhost:8082"), Logger.Level.FATAL);
        }
        catch ( Exception e)
        {
            assertTrue( false, String.format("Got error: %s", e.getMessage()) );
        }
    }
}
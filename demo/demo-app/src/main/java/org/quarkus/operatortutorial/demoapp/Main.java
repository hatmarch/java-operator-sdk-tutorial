package org.quarkus.operatortutorial.demoapp;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.arc.log.LoggerName;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        System.out.println("Running main method");
        Quarkus.run(args);
    }

    // FIXME: This gets injected too late?
    @ConfigProperty(name = "demo-app.logger-name")
    String loggerName;

    // driven by config property so can't inject
    Logger log;

    public Main() {
        log = Logger.getLogger(ConfigProvider.getConfig().getValue("demo-app.logger-name", String.class));
    }

    private Thread loggingThread;

    class LoggingThread extends Thread {

        public LoggingThread( String name )
        {
            super( name );
        }

        @Override
        public void run() {
            while (true) {
                logAllTypes();

                // wait a second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.info("The application is interrupted..!");
                    e.printStackTrace();
                    break;
                }
            }
            log.info("Finished Logging");
        }
    }

    void onStart(@Observes StartupEvent ev) {
        log.info("The application is starting...");

        loggingThread = new LoggingThread("Logger");
        loggingThread.start();
    }


    void onStop(@Observes ShutdownEvent ev) {               
        log.info("The application is stopping...");
        //loggingThread.interrupt();
    }


    private void logAllTypes() {
        log.fatal("This is fatal message");
        log.error("This is an error message");
        log.warn("This is a warn message");
        log.debug("This is a debug message");
        log.trace("This is a trace message");
        log.info("this is an info message");
    }
}

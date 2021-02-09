package org.mhildenb.operatortutorial.demooperator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javax.inject.Inject;

@QuarkusMain
public class QuarkusOperator implements QuarkusApplication {

  @Inject
  KubernetesClient client;

  // This class must be injected for the java operator SDK to startup and handle
  // the registration of customresource controllers
  @Inject
  Operator operator;

  @Inject
  ConfigurationService configuration;

  @Inject
  AppOpsController controller;

  private boolean _running = false;

  public static void main(String... args) {
    Quarkus.run(QuarkusOperator.class, args);
  }

  @Override
  public int run(String... args) throws Exception {
    final var config = configuration.getConfigurationFor(controller);
    System.out.println("CR class: " + config.getCustomResourceClass());
    _running = true;
    Quarkus.waitForExit();
    _running = false;
    return 0;
  }

  public boolean isRunning() {
    return _running;
  }
}

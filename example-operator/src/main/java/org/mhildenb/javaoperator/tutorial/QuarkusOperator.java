package org.mhildenb.javaoperator.tutorial;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javax.inject.Inject;

@QuarkusMain
public class QuarkusOperator implements QuarkusApplication {

  @Inject KubernetesClient client;

  @Inject Operator operator;

  @Inject ConfigurationService configuration;

  @Inject CustomServiceController controller;

  private boolean _running=false;

  public static void main(String... args) {
    Quarkus.run(QuarkusOperator.class, args);
  }

  @Override
  public int run(String... args) throws Exception {
    final var config = configuration.getConfigurationFor(controller);
    System.out.println("CR class: " + config.getCustomResourceClass());
    System.out.println("Doneable class = " + config.getDoneableClass());
    _running=true;
    Quarkus.waitForExit();
    _running=false;
    return 0;
  }

public boolean isRunning() {
	return _running;
}
}

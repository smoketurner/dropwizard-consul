package com.smoketurner.dropwizard.consul.example;

import com.smoketurner.dropwizard.consul.ConsulBundle;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class HelloWorldApplication
        extends Application<HelloWorldConfiguration> {

    public static void main(String[] args) throws Exception {
        new HelloWorldApplication().run(args);
    }

    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
        bootstrap.addBundle(new ConsulBundle<HelloWorldConfiguration>() {
            @Override
            public ConsulFactory getConsulFactory(
                    HelloWorldConfiguration configuration) {
                return configuration.getConsulFactory();
            }
        });
    }

    @Override
    public void run(HelloWorldConfiguration configuration,
            Environment environment) throws Exception {
        final HelloWorldResource resource = new HelloWorldResource(
                configuration.getTemplate(), configuration.getDefaultName());
        environment.jersey().register(resource);
    }
}

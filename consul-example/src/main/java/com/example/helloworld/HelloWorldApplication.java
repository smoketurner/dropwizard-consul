package com.example.helloworld;

import com.example.helloworld.resources.HelloWorldResource;
import com.orbitz.consul.Consul;
import com.smoketurner.dropwizard.consul.ConsulBundle;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import com.smoketurner.dropwizard.consul.ribbon.RibbonClient;
import com.smoketurner.dropwizard.consul.ribbon.RibbonClientBuilder;
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
        bootstrap.addBundle(new ConsulBundle<HelloWorldConfiguration>(getName()) {
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

        final Consul consul = configuration.getConsulFactory().build();
        final RibbonClientBuilder builder = new RibbonClientBuilder(environment,
                consul);
        final RibbonClient loadBalancingClient = builder
                .build(configuration.getDownstream());

        final HelloWorldResource resource = new HelloWorldResource(consul,
                loadBalancingClient, configuration.getTemplate(),
                configuration.getDefaultName());
        environment.jersey().register(resource);
    }
}

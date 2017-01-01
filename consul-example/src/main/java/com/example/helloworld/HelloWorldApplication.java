/**
 * Copyright 2017 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.helloworld;

import com.example.helloworld.resources.HelloWorldResource;
import com.orbitz.consul.Consul;
import com.smoketurner.dropwizard.consul.ConsulBundle;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import com.smoketurner.dropwizard.consul.ribbon.RibbonJerseyClient;
import com.smoketurner.dropwizard.consul.ribbon.RibbonJerseyClientBuilder;
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
        bootstrap.addBundle(
                new ConsulBundle<HelloWorldConfiguration>(getName()) {
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
        final RibbonJerseyClient loadBalancingClient = new RibbonJerseyClientBuilder(
                environment, consul, configuration.getClient())
                        .build("hello-world");

        final HelloWorldResource resource = new HelloWorldResource(consul,
                loadBalancingClient, configuration.getTemplate(),
                configuration.getDefaultName());
        environment.jersey().register(resource);
    }
}

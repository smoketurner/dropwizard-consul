/**
 * Copyright 2016 Smoke Turner, LLC.
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
package com.smoketurner.dropwizard.consul;

import java.util.concurrent.ScheduledExecutorService;
import com.orbitz.consul.Consul;
import com.smoketurner.dropwizard.consul.config.ConsulSubstitutor;
import com.smoketurner.dropwizard.consul.core.ConsulAdvertiser;
import com.smoketurner.dropwizard.consul.core.ConsulServiceCheckTask;
import com.smoketurner.dropwizard.consul.core.ConsulServiceListener;
import com.smoketurner.dropwizard.consul.health.ConsulHealthCheck;
import com.smoketurner.dropwizard.consul.jersey.ConsulBinder;
import com.smoketurner.dropwizard.consul.managed.ConsulAdvertiserManager;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

public abstract class ConsulBundle<C extends Configuration>
        implements ConfiguredBundle<C>, ConsulConfiguration<C> {

    // Default to creating a consul client that connects on localhost:8500 for
    // configuration variable substitution
    private Consul consul = Consul.builder().build();

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // replace variables with values from Consul KV
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new ConsulSubstitutor(consul, false)));
    }

    @Override
    public void run(C configuration, Environment environment) throws Exception {
        final ConsulFactory consulConfig = getConsulFactory(configuration);
        consul = consulConfig.build();

        // Register Consul with Jersey so we can get it from the request context
        environment.jersey().register(new ConsulBinder(consul));

        final ConsulAdvertiser advertiser = new ConsulAdvertiser(consulConfig,
                consul);

        // Register a Jetty listener to get the listening host and port
        environment.lifecycle().addServerLifecycleListener(
                new ConsulServiceListener(advertiser));

        final ScheduledExecutorService executor = environment.lifecycle()
                .scheduledExecutorService("consul-healthcheck", true).threads(1)
                .build();

        // Scheduled a periodic check to Consul to keep service alive
        final Duration interval = consulConfig.getCheckInterval();
        executor.scheduleAtFixedRate(
                new ConsulServiceCheckTask(consul,
                        ConsulAdvertiser.getServiceId()),
                1, interval.getQuantity(), interval.getUnit());

        // Register a ping healthcheck to the Consul agent
        environment.healthChecks().register("consul",
                new ConsulHealthCheck(consul));

        // Register a shutdown manager to deregister the service
        environment.lifecycle().manage(new ConsulAdvertiserManager(advertiser));
    }

    public Consul getConsul() {
        return consul;
    }
}

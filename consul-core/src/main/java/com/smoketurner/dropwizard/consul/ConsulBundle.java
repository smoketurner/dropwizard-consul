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

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.smoketurner.dropwizard.consul.config.ConsulSubstitutor;
import com.smoketurner.dropwizard.consul.core.ConsulAdvertiser;
import com.smoketurner.dropwizard.consul.core.ConsulServiceCheckTask;
import com.smoketurner.dropwizard.consul.core.ConsulServiceListener;
import com.smoketurner.dropwizard.consul.health.ConsulHealthCheck;
import com.smoketurner.dropwizard.consul.managed.ConsulAdvertiserManager;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

/**
 * Replace variables with values from Consul KV. By default, this only works
 * with a Consul agent running on localhost:8500 (the default) as there's no
 * way to configure Consul in the initialize methods.  You may override
 * {@link #getConsulAgentHost()} and {@link #getConsulAgentPort()} to provide
 * other defaults.
 *
 * @param <C> The configuration class for your Dropwizard Application.
 */
public abstract class ConsulBundle<C extends Configuration>
        implements ConfiguredBundle<C>, ConsulConfiguration<C> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulBundle.class);
    private static final long INITIAL_DELAY_SECS = 1;
    private final String serviceName;

    /**
     * Constructor
     *
     * @param name
     *            Service Name
     */
    public ConsulBundle(@Nonnull final String name) {
        this.serviceName = Objects.requireNonNull(name);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // Replace variables with values from Consul KV. This only works with a
        // Consul agent running on localhost:8500 (the default) as there's no
        // way to configure Consul in the initialize methods.
        try {
            bootstrap.setConfigurationSourceProvider(
                    new SubstitutingSourceProvider(
                            bootstrap.getConfigurationSourceProvider(),
                            new ConsulSubstitutor(Consul.builder()
                                    .withHostAndPort(HostAndPort.fromParts(
                                            getConsulAgentHost(),
                                            getConsulAgentPort()))
                                    .build(),
                                    false)));
        } catch (ConsulException e) {
            LOGGER.warn(String.format("Unable to query Consul running on %s:%s,"
                    + " disabling configuration subsitution", getConsulAgentHost(),
                    getConsulAgentPort()), e);
        }
    }

    @Override
    public void run(C configuration, Environment environment) throws Exception {
        final ConsulFactory consulConfig = getConsulFactory(configuration);
        consulConfig.setSeviceName(serviceName);
        final Consul consul = consulConfig.build();

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
                INITIAL_DELAY_SECS, interval.getQuantity(), interval.getUnit());

        // Register a ping healthcheck to the Consul agent
        environment.healthChecks().register("consul",
                new ConsulHealthCheck(consul));

        // Register a shutdown manager to deregister the service
        environment.lifecycle().manage(new ConsulAdvertiserManager(advertiser));
    }

	/**
     * Override as necessary to provide an alternative Consul Agent Host
     * @return By default, "localhost".
     */
    @VisibleForTesting
    String getConsulAgentHost() {
        return "localhost";
    }

	/**
     * Overrider as necessary to provide an alternative Consul Agent Port
     * @return By default, 8500.
     */
    @VisibleForTesting
    int getConsulAgentPort() {
        return 8500;
    }
}

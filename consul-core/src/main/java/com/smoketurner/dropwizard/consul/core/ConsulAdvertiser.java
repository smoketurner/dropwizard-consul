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
package com.smoketurner.dropwizard.consul.core;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.smoketurner.dropwizard.consul.ConsulFactory;

public class ConsulAdvertiser {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulAdvertiser.class);
    private static final String SERVICE_ID = UUID.randomUUID().toString();
    private final ConsulFactory configuration;
    private final Consul consul;

    private Optional<String> serviceHost = Optional.absent();
    private Optional<Integer> servicePort = Optional.absent();

    /**
     * Constructor
     *
     * @param configuration
     *            Consul configuration
     * @param consul
     *            Consul client
     */
    public ConsulAdvertiser(@Nonnull final ConsulFactory configuration,
            @Nonnull final Consul consul) {
        this.configuration = Objects.requireNonNull(configuration);
        this.consul = Objects.requireNonNull(consul);

        if (configuration.getServiceHost().isPresent()) {
            LOGGER.info("Using \"{}\" as serviceHost from configuration file",
                    configuration.getServiceHost().get());
            serviceHost = configuration.getServiceHost();
        }
        if (configuration.getServicePort().isPresent()) {
            LOGGER.info("Using \"{}\" as servicePort from configuration file",
                    configuration.getServicePort().get());
            servicePort = configuration.getServicePort();
        }
    }

    /**
     * Return the randomly generate Service ID
     *
     * @return random service ID
     */
    public static String getServiceId() {
        return SERVICE_ID;
    }

    /**
     * Initialize the advertiser by setting the host and port to register with
     * Consul. This method is called by {@link ConsulServiceListener} after
     * Jetty has been started.
     * 
     * @param host
     *            Service host to register
     * @param port
     *            Service port to register
     */
    public void initialize(@Nullable final String host,
            @Nullable final Integer port) {
        if (!serviceHost.isPresent()) {
            serviceHost = Optional.fromNullable(host);
        }
        if (!servicePort.isPresent()) {
            servicePort = Optional.fromNullable(port);
        }
    }

    /**
     * Register the service with Consul
     */
    public void register() {
        Preconditions.checkState(serviceHost.isPresent(),
                "serviceHost not set");
        Preconditions.checkState(servicePort.isPresent(),
                "servicePort not set");

        final AgentClient agent = consul.agentClient();
        if (agent.isRegistered(SERVICE_ID)) {
            LOGGER.info("Service ({}) with ID \"{}\" already registered",
                    configuration.getServiceName(), SERVICE_ID);
            return;
        }

        LOGGER.info(
                "Registering service (name={}, id={}) at <{}:{}> with a TTL check of {} seconds",
                configuration.getServiceName(), SERVICE_ID, serviceHost.get(),
                servicePort.get(), configuration.getServiceTTL().toSeconds());

        try {
            consul.agentClient().register(servicePort.get(),
                    configuration.getServiceTTL().toSeconds(),
                    configuration.getServiceName(), SERVICE_ID);
        } catch (ConsulException e) {
            LOGGER.error("Failed to register service in Consul", e);
        }
    }

    /**
     * Deregister a service from Consul
     */
    public void deregister() {
        final AgentClient agent = consul.agentClient();
        if (!agent.isRegistered(SERVICE_ID)) {
            LOGGER.info("No service registered with ID \"{}\"", SERVICE_ID);
            return;
        }

        LOGGER.info("Deregistering service ID \"{}\"", SERVICE_ID);

        try {
            consul.agentClient().deregister(SERVICE_ID);
        } catch (ConsulException e) {
            LOGGER.error("Failed to deregister service from Consul", e);
        }
    }
}

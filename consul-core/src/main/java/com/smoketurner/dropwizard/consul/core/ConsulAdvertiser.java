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

import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
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

    private Optional<Integer> servicePort = Optional.absent();
    private Optional<String> serviceAddress = Optional.absent();

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

        if (configuration.getServicePort().isPresent()) {
            LOGGER.info("Using \"{}\" as servicePort from configuration file",
                    configuration.getServicePort().get());
            servicePort = configuration.getServicePort();
        }

        if (configuration.getServiceAddress().isPresent()) {
            LOGGER.info("Using \"{}\" as serviceAddress from configuration file",
                configuration.getServiceAddress().get());
            serviceAddress = configuration.getServiceAddress();
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
     * Register the service with Consul
     *
     * @param port
     *            Port the service is listening on
     */
    public void register(final int port) {
        final AgentClient agent = consul.agentClient();
        if (agent.isRegistered(SERVICE_ID)) {
            LOGGER.info("Service ({}) [{}] already registered",
                    configuration.getServiceName(), SERVICE_ID);
            return;
        }

        // If we haven't set the servicePort via the configuration file already,
        // set it from the listening port.
        if (!servicePort.isPresent()) {
            servicePort = Optional.of(port);
        }

        LOGGER.info(
                "Registering service ({}) [{}] on port {} with a TTL check of {}s",
                configuration.getServiceName(), SERVICE_ID, servicePort.get(),
                configuration.getServiceTTL().toSeconds());

        try {
            Registration.RegCheck check = Registration.RegCheck.ttl(configuration.getServiceTTL().toSeconds());

            ImmutableRegistration.Builder registrationBuilder = ImmutableRegistration
                .builder()
                .port(servicePort.get())
                .check(check)
                .name(configuration.getServiceName())
                .id(SERVICE_ID);

            // If we have set the serviceAddress via the configuration file,
            // add it to the registration.
            if (serviceAddress.isPresent()) {
                registrationBuilder.address(serviceAddress.get());
            }

            Registration registration = registrationBuilder.build();

            consul.agentClient().register(registration);
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

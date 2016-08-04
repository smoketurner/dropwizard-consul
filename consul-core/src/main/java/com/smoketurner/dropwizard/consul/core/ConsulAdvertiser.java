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

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ConsulAdvertiser {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulAdvertiser.class);
    private final AtomicReference<Integer> servicePort = new AtomicReference<>();
    private  final AtomicReference<Integer> serviceAdminPort = new AtomicReference<>();
    private final AtomicReference<String> serviceAddress = new AtomicReference<>();
    private final AtomicReference<Iterable<String>> tags = new AtomicReference<>();
    private final ConsulFactory configuration;
    private final Consul consul;
    private final String serviceId;

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
        this(configuration, consul, UUID.randomUUID().toString());
    }

    /**
     * Constructor
     *
     * @param configuration
     *            Consul configuration
     * @param consul
     *            Consul client
     * @param serviceId
     *            Consul service ID
     */
    public ConsulAdvertiser(@Nonnull final ConsulFactory configuration,
            @Nonnull final Consul consul, @Nonnull final String serviceId) {
        this.configuration = Objects.requireNonNull(configuration);
        this.consul = Objects.requireNonNull(consul);
        this.serviceId = Objects.requireNonNull(serviceId);

        if (configuration.getServicePort().isPresent()) {
            LOGGER.info("Using \"{}\" as servicePort from configuration file",
                    configuration.getServicePort().get());
            servicePort.set(configuration.getServicePort().get());
        }

        if (configuration.getAdminPort().isPresent()) {
            LOGGER.info("Using \"{}\" as adminPort from configuration file",
                configuration.getAdminPort().get());
            serviceAdminPort.set(configuration.getAdminPort().get());
        }

        if (configuration.getServiceAddress().isPresent()) {
            LOGGER.info(
                    "Using \"{}\" as serviceAddress from configuration file",
                    configuration.getServiceAddress().get());
            serviceAddress.set(configuration.getServiceAddress().get());
        }

        if (configuration.getTags().isPresent()) {
            LOGGER.info("Using \"{}\" as tags from the configuration file",
                    configuration.getTags().get());
            tags.set(configuration.getTags().get());
        }
    }

    /**
     * Return the Service ID
     *
     * @return service ID
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Register the service with Consul
     *
     * @param applicationPort
     *            Port the service is listening on
     * @param adminPort
     *            Port the admin server is listening on
     */
    public void register(final int applicationPort, final int adminPort) {
        final AgentClient agent = consul.agentClient();
        if (agent.isRegistered(serviceId)) {
            LOGGER.info("Service ({}) [{}] already registered",
                    configuration.getServiceName(), serviceId);
            return;
        }

        // If we haven't set the servicePort via the configuration file already,
        // set it from the listening applicationPort.
        servicePort.compareAndSet(null, applicationPort);
        serviceAdminPort.compareAndSet(null, adminPort);

        LOGGER.info(
                "Registering service ({}) [{}] on applicationPort {} with a health check of {}s on {} adminPort",
                configuration.getServiceName(), serviceId, servicePort.get(),
                configuration.getCheckInterval().toSeconds(), serviceAdminPort.get());

        final Registration.RegCheck check = Registration.RegCheck.http(
            buildHealthCheckUrl(serviceAdminPort.get()), configuration.getCheckInterval().toSeconds());

        final ImmutableRegistration.Builder builder = ImmutableRegistration
                .builder().port(servicePort.get()).check(check)
                .name(configuration.getServiceName()).id(serviceId);

        // If we have set the serviceAddress, add it to the registration.
        if (serviceAddress.get() != null) {
            builder.address(serviceAddress.get());
        }

        // If we have tags, add them to the registration.
        if (tags.get() != null) {
            builder.tags(tags.get());
        }

        try {
            consul.agentClient().register(builder.build());
        } catch (ConsulException e) {
            LOGGER.error("Failed to register service in Consul", e);
        }
    }

    /**
     * Deregister a service from Consul
     */
    public void deregister() {
        final AgentClient agent = consul.agentClient();
        if (!agent.isRegistered(serviceId)) {
            LOGGER.info("No service registered with ID \"{}\"", serviceId);
            return;
        }

        LOGGER.info("Deregistering service ID \"{}\"", serviceId);

        try {
            consul.agentClient().deregister(serviceId);
        } catch (ConsulException e) {
            LOGGER.error("Failed to deregister service from Consul", e);
        }
    }

    private String buildHealthCheckUrl(int adminPort) {
        StringBuilder healthCheckUrl = new StringBuilder("http://");
        if (serviceAddress.get() == null) {
            healthCheckUrl.append("localhost");
        } else {
            healthCheckUrl.append(serviceAddress.get());
        }
        healthCheckUrl.append(":");
        healthCheckUrl.append(adminPort);
        healthCheckUrl.append("/healthcheck");

        return healthCheckUrl.toString();
    }
}

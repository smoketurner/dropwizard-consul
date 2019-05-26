/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import io.dropwizard.setup.Environment;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsulAdvertiser {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulAdvertiser.class);
  private final AtomicReference<Integer> servicePort = new AtomicReference<>();
  private final AtomicReference<Integer> serviceAdminPort = new AtomicReference<>();
  private final AtomicReference<String> serviceAddress = new AtomicReference<>();
  private final AtomicReference<String> aclToken = new AtomicReference<>();
  private final AtomicReference<Iterable<String>> tags = new AtomicReference<>();
  private final AtomicReference<Map<String, String>> serviceMeta = new AtomicReference<>();
  private final Environment environment;
  private final ConsulFactory configuration;
  private final Consul consul;
  private final String serviceId;

  /**
   * Constructor
   *
   * @param environment Dropwizard environment
   * @param configuration Consul configuration
   * @param consul Consul client
   * @param serviceId Consul service ID
   */
  public ConsulAdvertiser(
      final Environment environment,
      final ConsulFactory configuration,
      final Consul consul,
      final String serviceId) {

    this.environment = Objects.requireNonNull(environment, "environment == null");
    this.configuration = Objects.requireNonNull(configuration, "configuration == null");
    this.consul = Objects.requireNonNull(consul, "consul == null");
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId == null");

    configuration
        .getServicePort()
        .ifPresent(
            port -> {
              LOGGER.info("Using \"{}\" as servicePort from configuration file", port);
              servicePort.set(port);
            });

    configuration
        .getAdminPort()
        .ifPresent(
            port -> {
              LOGGER.info("Using \"{}\" as adminPort from configuration file", port);
              serviceAdminPort.set(port);
            });

    configuration
        .getServiceAddress()
        .ifPresent(
            address -> {
              LOGGER.info("Using \"{}\" as serviceAddress from configuration file", address);
              serviceAddress.set(address);
            });

    configuration
        .getTags()
        .ifPresent(
            newTags -> {
              LOGGER.info("Using \"{}\" as tags from the configuration file", newTags);
              tags.set(newTags);
            });

    configuration
        .getAclToken()
        .ifPresent(
            token -> {
              LOGGER.info("Using \"{}\" as ACL token from the configuration file.", token);
              aclToken.set(token);
            });

    configuration
        .getServiceMeta()
        .ifPresent(
            newServiceMeta -> {
              LOGGER.info(
                  "Using \"{}\" as serviceMeta from the configuration file", newServiceMeta);
              serviceMeta.set(newServiceMeta);
            });
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
   * @param applicationScheme Scheme the server is listening on
   * @param applicationPort Port the service is listening on
   * @param adminPort Port the admin server is listening on
   * @throws ConsulException When registration fails
   */
  public boolean register(
      final String applicationScheme, final int applicationPort, final int adminPort) {
    final AgentClient agent = consul.agentClient();
    if (agent.isRegistered(serviceId)) {
      LOGGER.info(
          "Service ({}) [{}] already registered", configuration.getServiceName(), serviceId);
      return false;
    }

    // If we haven't set the servicePort via the configuration file already,
    // set it from the listening applicationPort.
    servicePort.compareAndSet(null, applicationPort);
    serviceAdminPort.compareAndSet(null, adminPort);

    LOGGER.info(
        "Registering service ({}) [{}] on port {} (admin port {}) with a health check of {}s",
        configuration.getServiceName(),
        serviceId,
        servicePort.get(),
        serviceAdminPort.get(),
        configuration.getCheckInterval().toSeconds());

    final Registration.RegCheck check =
        ImmutableRegCheck.builder()
            .http(getHealthCheckUrl())
            .interval(String.format("%ds", configuration.getCheckInterval().toSeconds()))
            .deregisterCriticalServiceAfter(
                String.format("%dm", configuration.getDeregisterInterval().toMinutes()))
            .build();

    final ImmutableRegistration.Builder builder =
        ImmutableRegistration.builder().port(servicePort.get()).check(check).id(serviceId);

    final String serviceName = configuration.getServiceName();
    if (serviceName != null) {
      builder.name(serviceName);
    }

    // If we have set the serviceAddress, add it to the registration.
    if (serviceAddress.get() != null) {
      builder.address(serviceAddress.get());
    }

    // If we have tags, add them to the registration.
    if (tags.get() != null) {
      builder.tags(tags.get());
    }

    // If we have service meta, add them to the registration.
    if (serviceMeta.get() != null) {
      builder.meta(serviceMeta.get());
    }

    builder.putMeta("scheme", applicationScheme);

    consul.agentClient().register(builder.build());
    return true;
  }

  /** Deregister a service from Consul */
  public void deregister() {
    final AgentClient agent = consul.agentClient();
    try {
      if (!agent.isRegistered(serviceId)) {
        LOGGER.info("No service registered with ID \"{}\"", serviceId);
        return;
      }
    } catch (ConsulException e) {
      LOGGER.error("Failed to determine if service ID \"{}\" is registered", e);
      return;
    }

    LOGGER.info("Deregistering service ID \"{}\"", serviceId);

    try {
      consul.agentClient().deregister(serviceId);
    } catch (ConsulException e) {
      LOGGER.error("Failed to deregister service from Consul", e);
    }
  }

  protected String getHealthCheckUrl() {
    final UriBuilder builder = UriBuilder.fromPath(environment.getAdminContext().getContextPath());
    builder.path("healthcheck");
    builder.scheme("http");
    if (serviceAddress.get() == null) {
      builder.host("127.0.0.1");
    } else {
      builder.host(serviceAddress.get());
    }
    builder.port(serviceAdminPort.get());
    return builder.build().toString();
  }
}

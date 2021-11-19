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

import static java.util.Objects.nonNull;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import io.dropwizard.setup.Environment;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsulAdvertiser {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulAdvertiser.class);
  private static final String LOCALHOST = "127.0.0.1";
  private static final String DEFAULT_HEALTH_CHECK_PATH = "healthcheck";

  private final AtomicReference<Integer> servicePort = new AtomicReference<>();
  private final AtomicReference<Integer> serviceAdminPort = new AtomicReference<>();
  private final AtomicReference<String> serviceAddress = new AtomicReference<>();
  private final AtomicReference<String> serviceSubnet = new AtomicReference<>();
  private final AtomicReference<Supplier<String>> serviceAddressSupplier = new AtomicReference<>();
  private final AtomicReference<String> aclToken = new AtomicReference<>();
  private final AtomicReference<Iterable<String>> tags = new AtomicReference<>();
  private final AtomicReference<Map<String, String>> serviceMeta = new AtomicReference<>();
  private final Environment environment;
  private final ConsulFactory configuration;
  private final Consul consul;
  private final String serviceId;
  private final AtomicReference<String> healthCheckPath = new AtomicReference<>();

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
        .getServiceSubnet()
        .ifPresent(
            subnet -> {
              LOGGER.info("Using \"{}\" as serviceSubnet from configuration file", subnet);
              serviceSubnet.set(subnet);
            });

    configuration
        .getServiceAddressSupplier()
        .ifPresent(
            supplier -> {
              LOGGER.info("Using \"{}\" as serviceSupplier from configuration file", supplier);
              serviceAddressSupplier.set(supplier);
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

      configuration
          .getHealthCheckPath()
          .ifPresent(
              newHealthCheckPath -> {
                  LOGGER.info(
                      "Using \"{}\" as health check path from the configuration file", newHealthCheckPath);
                  healthCheckPath.set(newHealthCheckPath);
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

  public boolean register(
      final String applicationScheme, final int applicationPort, final int adminPort) {
    return register(applicationScheme, applicationPort, adminPort, null);
  }

  /**
   * Register the service with Consul
   *
   * @param applicationScheme Scheme the server is listening on
   * @param applicationPort Port the service is listening on
   * @param adminPort Port the admin server is listening on
   * @param ipAddresses IP addresses of the available that the application is listening on
   * @return true if successfully registered, otherwise false
   * @throws ConsulException When registration fails
   */
  public boolean register(
      final String applicationScheme,
      final int applicationPort,
      final int adminPort,
      Collection<String> ipAddresses) {
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
    healthCheckPath.compareAndSet(null, DEFAULT_HEALTH_CHECK_PATH);

    LOGGER.info(
        "Registering service ({}) [{}] on port {} (admin port {}) with a health check at {} with interval of {}s",
        configuration.getServiceName(),
        serviceId,
        servicePort.get(),
        serviceAdminPort.get(),
        healthCheckPath.get(),
        configuration.getCheckInterval().toSeconds());

    final Registration.RegCheck check =
        ImmutableRegCheck.builder()
            .http(getHealthCheckUrl(applicationScheme, ipAddresses))
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
    getServiceAddress(ipAddresses).ifPresent(builder::address);

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

  /**
   * Returns the service address from best provided options. The order of precedence is as follows:
   * serviceAddress, if provided, then the subnet resolution, lastly the supplier. If none of the
   * above is provided or matched, Optional.empty() is returned.
   *
   * @param ipAddresses List of ipAddresses the application is listening on.
   * @return Optional of the host to register as the service address or empty otherwise
   */
  private Optional<String> getServiceAddress(Collection<String> ipAddresses) {
    if (nonNull(serviceAddress.get())) {
      return Optional.of(serviceAddress.get());
    }

    if (nonNull(ipAddresses) && !ipAddresses.isEmpty() && nonNull(serviceSubnet.get())) {
      Optional<String> ip = findFirstEligibleIpBySubnet(ipAddresses);
      if (ip.isPresent()) {
        return ip;
      }
    }

    if (nonNull(serviceAddressSupplier.get())) {
      try {
        return Optional.ofNullable(serviceAddressSupplier.get().get());
      } catch (Exception ex) {
        LOGGER.debug("Service address supplier threw an exception.", ex);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the service address from the list of hosts. It iterates through the list and finds the
   * first host tht matched the subnet. If none is found, an empty Optional is returned.
   *
   * @param ipAddresses List of ipAddresses the application is listening on.
   * @return Optional of the host to register as the service address or empty otherwise
   */
  private Optional<String> findFirstEligibleIpBySubnet(Collection<String> ipAddresses) {
    SubnetUtils subnetUtils = new SubnetUtils(serviceSubnet.get());
    SubnetUtils.SubnetInfo subNetInfo = subnetUtils.getInfo();
    return ipAddresses.stream().filter(subNetInfo::isInRange).findFirst();
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
      LOGGER.error("Failed to determine if service ID \"{}\" is registered", serviceId, e);
      return;
    }

    LOGGER.info("Deregistering service ID \"{}\"", serviceId);

    try {
      consul.agentClient().deregister(serviceId);
    } catch (ConsulException e) {
      LOGGER.error("Failed to deregister service from Consul", e);
    }
  }

  /**
   * Return the health check URL for the service
   *
   * @param applicationScheme Scheme the server is listening on
   * @return health check URL
   */
  protected String getHealthCheckUrl(String applicationScheme, Collection<String> hosts) {
    final UriBuilder builder = UriBuilder.fromPath(environment.getAdminContext().getContextPath());
    builder
        .path(healthCheckPath.get())
        .scheme(applicationScheme)
        .host(getServiceAddress(hosts).orElse(LOCALHOST))
        .port(serviceAdminPort.get());
    return builder.build().toString();
  }
}

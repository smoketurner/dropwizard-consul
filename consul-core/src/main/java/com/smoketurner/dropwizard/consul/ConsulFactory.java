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
package com.smoketurner.dropwizard.consul;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.net.util.SubnetUtils;

public class ConsulFactory {
  private static final String CONSUL_AUTH_HEADER_KEY = "X-Consul-Token";

  @NotNull
  private HostAndPort endpoint =
      HostAndPort.fromParts(Consul.DEFAULT_HTTP_HOST, Consul.DEFAULT_HTTP_PORT);

  @Nullable private String serviceName;

  private boolean enabled = true;
  private Optional<String> serviceId = Optional.empty();
  private Optional<Integer> servicePort = Optional.empty();
  private Optional<Integer> adminPort = Optional.empty();
  private Optional<String> serviceAddress = Optional.empty();
  private Optional<String> serviceSubnet = Optional.empty();
  private Optional<Supplier<String>> serviceAddressSupplier = Optional.empty();
  private Optional<Iterable<String>> tags = Optional.empty();
  private Optional<String> aclToken = Optional.empty();
  private Optional<Map<String, String>> serviceMeta = Optional.empty();
  private boolean servicePing = true;

  @Nullable
  @MinDuration(value = 1, unit = TimeUnit.SECONDS)
  private Duration retryInterval;

  @NotNull
  @MinDuration(value = 1, unit = TimeUnit.SECONDS)
  private Duration checkInterval = Duration.seconds(1);

  @NotNull
  @MinDuration(value = 1, unit = TimeUnit.MINUTES)
  private Duration deregisterInterval = Duration.minutes(1);

  @JsonProperty
  public boolean isEnabled() {
    return enabled;
  }

  @JsonProperty
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @JsonProperty
  public HostAndPort getEndpoint() {
    return endpoint;
  }

  @JsonProperty
  public void setEndpoint(HostAndPort endpoint) {
    this.endpoint = endpoint;
  }

  @JsonProperty
  public Optional<String> getServiceId() {
    return serviceId;
  }

  @JsonProperty
  public void setServiceId(@Nullable String serviceId) {
    this.serviceId = Optional.ofNullable(serviceId);
  }

  @Nullable
  @JsonProperty
  public String getServiceName() {
    return serviceName;
  }

  @JsonProperty
  public void setServiceName(@Nullable String serviceName) {
    this.serviceName = serviceName;
  }

  @Deprecated
  @JsonProperty
  public void setSeviceName(@Nullable String serviceName) {
    this.serviceName = serviceName;
  }

  @JsonProperty
  public Optional<Iterable<String>> getTags() {
    return tags;
  }

  @JsonProperty
  public void setTags(Iterable<String> tags) {
    this.tags = Optional.ofNullable(tags);
  }

  @JsonProperty
  public Optional<Integer> getServicePort() {
    return servicePort;
  }

  @JsonProperty
  public void setServicePort(Integer servicePort) {
    this.servicePort = Optional.ofNullable(servicePort);
  }

  @JsonProperty
  public Optional<Integer> getAdminPort() {
    return adminPort;
  }

  @JsonProperty
  public void setAdminPort(Integer adminPort) {
    this.adminPort = Optional.ofNullable(adminPort);
  }

  @JsonProperty
  public Optional<String> getServiceAddress() {
    return serviceAddress;
  }

  @JsonProperty
  public void setServiceAddress(String serviceAddress) {
    this.serviceAddress = Optional.ofNullable(serviceAddress);
  }

  @JsonProperty
  public Optional<Duration> getRetryInterval() {
    return Optional.ofNullable(retryInterval);
  }

  @JsonProperty
  public void setRetryInterval(@Nullable Duration interval) {
    this.retryInterval = interval;
  }

  @JsonProperty
  public Duration getCheckInterval() {
    return checkInterval;
  }

  @JsonProperty
  public void setCheckInterval(Duration interval) {
    this.checkInterval = interval;
  }

  @JsonProperty
  public Duration getDeregisterInterval() {
    return deregisterInterval;
  }

  @JsonProperty
  public void setDeregisterInterval(Duration interval) {
    this.deregisterInterval = interval;
  }

  @JsonProperty
  public Optional<String> getAclToken() {
    return aclToken;
  }

  @JsonProperty
  public void setAclToken(@Nullable String aclToken) {
    this.aclToken = Optional.ofNullable(aclToken);
  }

  @JsonProperty
  public Optional<Map<String, String>> getServiceMeta() {
    return serviceMeta;
  }

  @JsonProperty
  public void setServiceMeta(Map<String, String> serviceMeta) {
    this.serviceMeta = Optional.ofNullable(serviceMeta);
  }

  @JsonProperty
  public boolean isServicePing() {
    return servicePing;
  }

  @JsonProperty
  public void setServicePing(boolean servicePing) {
    this.servicePing = servicePing;
  }

  public Optional<String> getServiceSubnet() {
    return serviceSubnet;
  }

  public void setServiceSubnet(String serviceSubnet) {
    Preconditions.checkArgument(
        isValidCidrIp(serviceSubnet), "%s is not a valid Subnet in CIDR notation", serviceSubnet);
    this.serviceSubnet = Optional.ofNullable(serviceSubnet);
  }

  public void setServiceAddressSupplier(Supplier<String> serviceAddressSupplier) {
    this.serviceAddressSupplier = Optional.ofNullable(serviceAddressSupplier);
  }

  public Optional<Supplier<String>> getServiceAddressSupplier() {
    return serviceAddressSupplier;
  }

  @JsonIgnore
  public Consul build() {

    final Consul.Builder builder = Consul.builder().withHostAndPort(endpoint).withPing(servicePing);

    aclToken.ifPresent(
        token -> {
          // setting both acl token here and with header, supplying an auth
          // header. This should cover both use cases: endpoint supports
          // legacy ?token query param and other case in which endpoint
          // requires an X-Consul-Token header.
          // @see https://www.consul.io/api/index.html#acls
          builder.withAclToken(token).withHeaders(ImmutableMap.of(CONSUL_AUTH_HEADER_KEY, token));
        });

    return builder.build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        endpoint,
        serviceName,
        enabled,
        servicePort,
        adminPort,
        serviceAddress,
        tags,
        retryInterval,
        checkInterval,
        deregisterInterval,
        aclToken,
        serviceMeta,
        servicePing);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ConsulFactory other = (ConsulFactory) obj;
    return Objects.equals(this.endpoint, other.endpoint)
        && Objects.equals(this.serviceName, other.serviceName)
        && Objects.equals(this.enabled, other.enabled)
        && Objects.equals(this.servicePort, other.servicePort)
        && Objects.equals(this.adminPort, other.adminPort)
        && Objects.equals(this.serviceAddress, other.serviceAddress)
        && Objects.equals(this.tags, other.tags)
        && Objects.equals(this.retryInterval, other.retryInterval)
        && Objects.equals(this.checkInterval, other.checkInterval)
        && Objects.equals(this.deregisterInterval, other.deregisterInterval)
        && Objects.equals(this.aclToken, other.aclToken)
        && Objects.equals(this.serviceMeta, other.serviceMeta)
        && Objects.equals(this.servicePing, other.servicePing);
  }

  private static boolean isValidCidrIp(String cidrIp) {
    boolean isValid = true;
    try {
      new SubnetUtils(cidrIp);
    } catch (IllegalArgumentException e) {
      isValid = false;
    }
    return isValid;
  }
}

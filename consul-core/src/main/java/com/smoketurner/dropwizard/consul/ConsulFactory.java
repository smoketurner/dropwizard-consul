/**
 * Copyright 2018 Smoke Turner, LLC.
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

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;

public class ConsulFactory {
    @NotNull
    private HostAndPort endpoint = HostAndPort
            .fromParts(Consul.DEFAULT_HTTP_HOST, Consul.DEFAULT_HTTP_PORT);

    @Nullable
    private String serviceName;

    private boolean enabled = true;
    private Optional<Integer> servicePort = Optional.empty();
    private Optional<Integer> adminPort = Optional.empty();
    private Optional<String> serviceAddress = Optional.empty();
    private Optional<Iterable<String>> tags = Optional.empty();
    private Optional<String> aclToken = Optional.empty();

    @NotNull
    @MinDuration(value = 1, unit = TimeUnit.SECONDS)
    private Duration checkInterval = Duration.seconds(1);

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

    @Nullable
    @JsonProperty
    public String getServiceName() {
        return serviceName;
    }

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
    public Duration getCheckInterval() {
        return checkInterval;
    }

    @JsonProperty
    public void setCheckInterval(Duration checkInterval) {
        this.checkInterval = checkInterval;
    }

    @JsonProperty
    public Optional<String> getAclToken() { return aclToken; }

    @JsonProperty
    public void setAclToken(@Nullable String aclToken){
        this.aclToken = Optional.ofNullable(aclToken);
    }

    @JsonIgnore
    public Consul build() {

        Consul.Builder builder = Consul.builder()
            .withHostAndPort(endpoint);

        if(aclToken.isPresent()){
            final String CONSUL_AUTH_HEADER_KEY = "X-Consul-Token";
            // setting both acl token here and with header, supplying an auth header. This should
            // cover both use cases - endpoint supports legacy ?token query param and other case
            // in which endpoint requires an X-Consul-Token header.
            // see: https://www.consul.io/api/index.html#acls
            Map<String, String> headers = ImmutableMap.of(CONSUL_AUTH_HEADER_KEY, aclToken.get());
            builder.withAclToken(aclToken.get())
                .withHeaders(headers);
        }

        return builder.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, serviceName, enabled, servicePort,
                adminPort, serviceAddress, tags, checkInterval, aclToken);
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
                && Objects.equals(this.checkInterval, other.checkInterval)
                && Objects.equals(this.aclToken, other.aclToken);
    }

}

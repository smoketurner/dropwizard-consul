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

import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.MinDuration;

public class ConsulFactory {
    @NotNull
    private HostAndPort endpoint = HostAndPort.fromString("127.0.0.1:8500");

    @NotEmpty
    private String serviceName;

    private Optional<String> serviceHost = Optional.absent();
    private Optional<Integer> servicePort = Optional.absent();

    @NotNull
    @MinDuration(value = 1, unit = TimeUnit.SECONDS)
    private Duration serviceTTL = Duration.seconds(3);

    @NotNull
    @MinDuration(value = 1, unit = TimeUnit.SECONDS)
    private Duration checkInterval = Duration.seconds(1);

    @JsonProperty
    public HostAndPort getEndpoint() {
        return endpoint;
    }

    @JsonProperty
    public void setEndpoint(HostAndPort endpoint) {
        this.endpoint = endpoint;
    }

    @JsonProperty
    public String getServiceName() {
        return serviceName;
    }

    @JsonProperty
    public void setSeviceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @JsonProperty
    public Optional<String> getServiceHost() {
        return serviceHost;
    }

    @JsonProperty
    public void setServiceHost(String serviceHost) {
        this.serviceHost = Optional.fromNullable(serviceHost);
    }

    @JsonProperty
    public Optional<Integer> getServicePort() {
        return servicePort;
    }

    @JsonProperty
    public void setServicePort(Integer servicePort) {
        this.servicePort = Optional.fromNullable(servicePort);
    }

    @JsonProperty("serviceTTL")
    public Duration getServiceTTL() {
        return serviceTTL;
    }

    @JsonProperty("serviceTTL")
    public void setServiceTTL(Duration serviceTTL) {
        this.serviceTTL = serviceTTL;
    }

    @JsonProperty
    public Duration getCheckInterval() {
        return checkInterval;
    }

    @JsonProperty
    public void setCheckInterval(Duration passInterval) {
        this.checkInterval = passInterval;
    }

    @JsonIgnore
    public Consul build() {
        return Consul.builder().withHostAndPort(endpoint).build();
    }
}

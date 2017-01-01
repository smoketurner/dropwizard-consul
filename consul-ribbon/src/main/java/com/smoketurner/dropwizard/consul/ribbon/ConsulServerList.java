/**
 * Copyright 2017 Smoke Turner, LLC.
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
package com.smoketurner.dropwizard.consul.ribbon;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.google.common.base.Strings;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;

public class ConsulServerList implements ServerList<Server> {

    private final Consul consul;
    private final ConsulServiceDiscoverer serviceDiscoverer;

    /**
     * Constructor
     *
     * @param consul
     *            Consul client
     * @param serviceDiscoverer
     *            Discoverer
     */
    public ConsulServerList(@Nonnull final Consul consul,
            @Nonnull final ConsulServiceDiscoverer serviceDiscoverer) {
        this.consul = Objects.requireNonNull(consul);
        this.serviceDiscoverer = Objects.requireNonNull(serviceDiscoverer);
    }

    @Override
    public List<Server> getInitialListOfServers() {
        return buildServerList(serviceDiscoverer.discover(consul));
    }

    @Override
    public List<Server> getUpdatedListOfServers() {
        return buildServerList(serviceDiscoverer.discover(consul));
    }

    /**
     * Converts a list of {@link ServiceHealth} objects into {@link Server}
     * objects
     *
     * @param services
     *            list of healthy service instances
     * @return list of server instances
     */
    private List<Server> buildServerList(
            @Nonnull final Collection<ServiceHealth> services) {
        return services.stream().map(this::buildServer)
                .collect(Collectors.toList());
    }

    /**
     * Build a {@link Server} instance from a Consul {@link ServiceHealth}
     * instance. If the service has an address defined, use that as the server
     * host, otherwise default to using the node address.
     *
     * @param service
     *            Consul service health record
     * @return Ribbon Server instance
     */
    private Server buildServer(final ServiceHealth service) {
        if (!Strings.isNullOrEmpty(service.getService().getAddress())) {
            return new Server(service.getService().getAddress(),
                    service.getService().getPort());
        } else {
            return new Server(service.getNode().getAddress(),
                    service.getService().getPort());
        }
    }
}

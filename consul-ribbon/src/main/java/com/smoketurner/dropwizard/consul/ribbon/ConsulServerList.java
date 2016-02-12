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
package com.smoketurner.dropwizard.consul.ribbon;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;

public class ConsulServerList implements ServerList<Server> {

    private final Consul consul;
    private final String service;
    private List<Server> serverList = Collections.emptyList();

    /**
     * Constructor
     *
     * @param consul
     *            Consul client
     * @param service
     *            Service name
     */
    public ConsulServerList(@Nonnull final Consul consul,
            @Nonnull final String service) {
        this(consul, service, Collections.emptyList());
    }

    /**
     * Constructor
     *
     * @param consul
     *            Consul client
     * @param service
     *            Service name
     * @param services
     *            Initial list of healthy services
     */
    public ConsulServerList(@Nonnull final Consul consul,
            @Nonnull final String service,
            @Nonnull final List<ServiceHealth> services) {
        this.consul = Objects.requireNonNull(consul);
        this.service = Objects.requireNonNull(service);
        this.serverList = buildServerList(Objects.requireNonNull(services));
    }

    @Override
    public List<Server> getInitialListOfServers() {
        return serverList;
    }

    @Override
    public List<Server> getUpdatedListOfServers() {
        final List<ServiceHealth> services = consul.healthClient()
                .getHealthyServiceInstances(service).getResponse();
        return buildServerList(services);
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
            @Nonnull final List<ServiceHealth> services) {
        return services.stream().map(service -> 
             new Server(service.getNode().getAddress(),
                    service.getService().getPort())
        ).collect(Collectors.toList());
    }
}

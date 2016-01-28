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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

public class RibbonClient implements Closeable {
    private final ZoneAwareLoadBalancer<Server> loadBalancer;
    private final Client client;

    /**
     * Constructor
     *
     * @param loadBalancer
     *            Load Balancer
     * @param client
     *            Jersey Client
     */
    public RibbonClient(
            @Nonnull final ZoneAwareLoadBalancer<Server> loadBalancer,
            @Nonnull final Client client) {
        this.loadBalancer = Objects.requireNonNull(loadBalancer);
        this.client = Objects.requireNonNull(client);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *             if there are no available servers
     */
    public WebTarget target() {
        return client.target(UriBuilder
                .fromPath("http://" + fetchServerOrThrow().getHostPort())
                .build());
    }

    /**
     * Return a list of available servers from this load balancing client
     *
     * @return a list of available servers
     */
    public List<Server> getAvailableServers() {
        return loadBalancer.getServerList(true);
    }

    /**
     * Fetch a server from the load balancer or throw an exception if none are
     * available.
     * 
     * @return a server
     * @throws IllegalStateException
     *             if no servers are available
     */
    private Server fetchServerOrThrow() {
        final Server server = loadBalancer.chooseServer();
        if (server == null) {
            throw new IllegalStateException(
                    "No available servers for " + loadBalancer.getName());
        }
        return server;
    }

    @Override
    public void close() throws IOException {
        loadBalancer.shutdown();
        client.close();
    }
}

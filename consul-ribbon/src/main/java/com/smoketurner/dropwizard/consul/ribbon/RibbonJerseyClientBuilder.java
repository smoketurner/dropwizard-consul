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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.orbitz.consul.Consul;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;

public class RibbonJerseyClientBuilder {

    private final Environment environment;
    private final Consul consul;
    private final RibbonLoadBalancerConfiguration configuration;

    /**
     * Constructor
     * 
     * @param environment
     *            Dropwizard environment
     * @param consul
     *            Consul client
     * @param configuration
     *            Configuration
     */
    public RibbonJerseyClientBuilder(@Nonnull final Environment environment,
            @Nonnull final Consul consul,
            @Nonnull final RibbonLoadBalancerConfiguration configuration) {
        this.environment = Objects.requireNonNull(environment);
        this.consul = Objects.requireNonNull(consul);
        this.configuration = Objects.requireNonNull(configuration);
    }

    /**
     * Builds a new {@link RibbonJerseyClient} using service discovery by health
     *
     * @param name
     *            Service name
     * @return new RibbonJerseyClient
     */
    public RibbonJerseyClient build(@Nonnull final String name) {
        return build(name, new HealthyConsulServiceDiscoverer(name));
    }

    /**
     * Builds a new {@link RibbonJerseyClient} using the provided service
     * discoverer
     *
     * @param clientName
     *            Jersey client name
     * @param serviceDiscoverer
     *            Service discoverer
     * @return new RibbonJerseyClient
     */
    public RibbonJerseyClient build(@Nonnull final String clientName,
            @Nonnull final ConsulServiceDiscoverer serviceDiscoverer) {

        // create a new Jersey client
        final Client jerseyClient = new JerseyClientBuilder(environment)
                .build(clientName);

        return build(clientName, jerseyClient, serviceDiscoverer);
    }

    /**
     * Builds a new {@link RibbonJerseyClient} using service discovery by health
     *
     * @param name
     *            Service name
     * @param jerseyClient
     *            Jersey Client
     * @return new {@link RibbonJerseyClient}
     */
    public RibbonJerseyClient build(@Nonnull final String name,
            @Nonnull final Client jerseyClient) {
        return build(name, jerseyClient,
                new HealthyConsulServiceDiscoverer(name));
    }

    /**
     * Builds a new {@link RibbonJerseyClient} with an existing Jersey Client
     * and service discoverer
     *
     * @param name
     *            Client name
     * @param jerseyClient
     *            Jersey Client
     * @param serviceDiscoverer
     *            Service discoverer
     * @return new RibbonJerseyClient
     */
    public RibbonJerseyClient build(@Nonnull final String name,
            @Nonnull final Client jerseyClient,
            @Nonnull final ConsulServiceDiscoverer serviceDiscoverer) {
        // build a new load balancer based on the configuration
        final RibbonLoadBalancerBuilder factory = new RibbonLoadBalancerBuilder(
                new ConsulServerList(name, consul, serviceDiscoverer));
        final ZoneAwareLoadBalancer<Server> loadBalancer = factory
                .build(configuration);

        final RibbonJerseyClient client = new RibbonJerseyClient(loadBalancer,
                jerseyClient);

        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                // nothing to start
            }

            @Override
            public void stop() throws Exception {
                client.close();
            }
        });
        return client;
    }
}

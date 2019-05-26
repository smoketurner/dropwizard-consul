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
package com.smoketurner.dropwizard.consul.ribbon;

import com.google.common.base.Strings;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class ConsulServerList implements ServerList<Server> {

  private final Consul consul;
  private final ConsulServiceDiscoverer serviceDiscoverer;

  /**
   * Constructor
   *
   * @param consul Consul client
   * @param serviceDiscoverer Discoverer
   */
  public ConsulServerList(final Consul consul, final ConsulServiceDiscoverer serviceDiscoverer) {
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
   * Converts a list of {@link ServiceHealth} objects into {@link Server} objects
   *
   * @param services list of healthy service instances
   * @return list of server instances
   */
  private List<Server> buildServerList(final Collection<ServiceHealth> services) {
    return services.stream().map(this::buildServer).collect(Collectors.toList());
  }

  /**
   * Build a {@link Server} instance from a Consul {@link ServiceHealth} instance. If the service
   * has an address defined, use that as the server host, otherwise default to using the node
   * address.
   *
   * @param service Consul service health record
   * @return Ribbon Server instance
   */
  private Server buildServer(final ServiceHealth service) {
    @Nullable final String scheme = service.getService().getMeta().get("scheme");
    final int port = service.getService().getPort();

    final String address;
    if (!Strings.isNullOrEmpty(service.getService().getAddress())) {
      address = service.getService().getAddress();
    } else {
      address = service.getNode().getAddress();
    }

    final Server server = new Server(scheme, address, port);
    server.setZone(service.getNode().getDatacenter().orElse(Server.UNKNOWN_ZONE));
    server.setReadyToServe(true);

    return server;
  }
}

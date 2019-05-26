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

import com.orbitz.consul.ConsulException;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.util.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsulServiceListener implements ServerLifecycleListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceListener.class);

  private static final String APPLICATION_NAME = "application";
  private static final String ADMIN_NAME = "admin";

  private final ConsulAdvertiser advertiser;
  private final Optional<Duration> retryInterval;
  private final Optional<ScheduledExecutorService> scheduler;

  /**
   * Constructor
   *
   * @param advertiser Consul advertiser
   * @param retryInterval When specified, will retry if service registration fails
   * @param scheduler When specified, will retry if service registration fails
   */
  public ConsulServiceListener(
      final ConsulAdvertiser advertiser,
      final Optional<Duration> retryInterval,
      final Optional<ScheduledExecutorService> scheduler) {

    this.advertiser = Objects.requireNonNull(advertiser, "advertiser == null");
    this.retryInterval = Objects.requireNonNull(retryInterval, "retryInterval == null");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler == null");
  }

  @Override
  public void serverStarted(final Server server) {

    String applicationScheme = null;
    int applicationPort = -1;
    int adminPort = -1;

    for (Connector connector : server.getConnectors()) {
      @SuppressWarnings("resource")
      final ServerConnector serverConnector = (ServerConnector) connector;
      if (APPLICATION_NAME.equals(connector.getName())) {
        applicationPort = serverConnector.getLocalPort();
        applicationScheme = getScheme(connector.getProtocols());
      } else if (ADMIN_NAME.equals(connector.getName())) {
        adminPort = serverConnector.getLocalPort();
      } else {
        applicationPort = serverConnector.getLocalPort();
        applicationScheme = getScheme(connector.getProtocols());
        adminPort = applicationPort;
      }
    }

    LOGGER.debug(
        "applicationScheme: {}, applicationPort: {}, adminPort: {}",
        applicationScheme,
        applicationPort,
        adminPort);

    register(applicationScheme, applicationPort, adminPort);
  }

  /**
   * Return the protocol scheme from a list of protocols.
   *
   * @param protocols Configured protocols
   * @return protocol scheme
   */
  private static String getScheme(List<String> protocols) {
    if (protocols.contains("ssl")) {
      return "https";
    }
    return "http";
  }

  /**
   * Register ports with Consul and retry if unavailable
   *
   * @param applicationScheme Application protocol scheme
   * @param applicationPort Application listening port
   * @param adminPort Administration listening port
   */
  void register(String applicationScheme, int applicationPort, int adminPort) {
    try {
      advertiser.register(applicationScheme, applicationPort, adminPort);
      scheduler.ifPresent(ScheduledExecutorService::shutdownNow);
    } catch (ConsulException e) {
      LOGGER.error("Failed to register service in Consul", e);

      retryInterval.ifPresent(
          (interval) -> {
            scheduler.ifPresent(
                (service) -> {
                  LOGGER.info(
                      "Will try to register service again in {} seconds", interval.toSeconds());
                  service.schedule(
                      () -> register(applicationScheme, applicationPort, adminPort),
                      interval.toSeconds(),
                      TimeUnit.SECONDS);
                });
          });
    }
  }
}

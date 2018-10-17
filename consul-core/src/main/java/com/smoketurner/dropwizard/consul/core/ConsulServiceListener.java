/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsulServiceListener implements ServerLifecycleListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceListener.class);
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
      @Nullable final Duration retryInterval,
      @Nullable final ScheduledExecutorService scheduler) {
    this.advertiser = Objects.requireNonNull(advertiser);
    this.retryInterval = Optional.ofNullable(retryInterval);
    this.scheduler = Optional.ofNullable(scheduler);
  }

  @Override
  public void serverStarted(final Server server) {
    final int applicationPort = getLocalPort(server);
    final int adminPort = getAdminPort(server);
    register(applicationPort, adminPort);
  }

  void register(int applicationPort, int adminPort) {
    try {
      advertiser.register(applicationPort, adminPort);
      if (scheduler.isPresent()) {
        scheduler.get().shutdown();
      }
    } catch (ConsulException e) {
      LOGGER.error("Failed to register service in Consul", e);
      if (scheduler.isPresent()) {
        LOGGER.info(
            "Will try to register service again in {} seconds", retryInterval.get().toSeconds());
        scheduler
            .get()
            .schedule(
                () -> register(applicationPort, adminPort),
                retryInterval.get().toSeconds(),
                TimeUnit.SECONDS);
      }
    }
  }
}

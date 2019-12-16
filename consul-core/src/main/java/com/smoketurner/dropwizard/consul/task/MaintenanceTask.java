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
package com.smoketurner.dropwizard.consul.task;

import com.google.common.base.Strings;
import com.orbitz.consul.Consul;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaintenanceTask extends Task {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceTask.class);
  private final Consul consul;
  private final String serviceId;

  /**
   * Constructor
   *
   * @param consul Consul client
   * @param serviceId Service ID to toggle maintenance mode
   */
  public MaintenanceTask(final Consul consul, final String serviceId) {
    super("maintenance");
    this.consul = Objects.requireNonNull(consul);
    this.serviceId = Objects.requireNonNull(serviceId);
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {

    if (!parameters.containsKey("enable")) {
      throw new IllegalArgumentException("Parameter \"enable\" not found");
    }

    final boolean enable = Boolean.parseBoolean(parameters.get("enable").get(0));
    final String reason;
    if (parameters.containsKey("reason")) {
      reason = Strings.nullToEmpty(parameters.get("reason").get(0));
    } else {
      reason = "";
    }

    if (enable) {
      if (!Strings.isNullOrEmpty(reason)) {
        LOGGER.warn("Enabling maintenance mode for service {} (reason: {})", serviceId, reason);
      } else {
        LOGGER.warn("Enabling maintenance mode for service {} (no reason given)", serviceId);
      }
    } else {
      LOGGER.warn("Disabling maintenance mode for service {}", serviceId);
    }

    consul.agentClient().toggleMaintenanceMode(serviceId, enable, reason);

    output.println("OK");
    output.flush();
  }
}

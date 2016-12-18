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
package com.smoketurner.dropwizard.consul.task;

import java.io.PrintWriter;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import io.dropwizard.servlets.tasks.Task;

public class MaintenanceTask extends Task {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MaintenanceTask.class);
    private final Consul consul;
    private final String serviceId;

    /**
     * Constructor
     *
     * @param consul
     *            Consul client
     * @param serviceId
     *            Service ID to toggle maintenance mode
     */
    public MaintenanceTask(@Nonnull final Consul consul,
            @Nonnull final String serviceId) {
        super("maintenance");
        this.consul = Objects.requireNonNull(consul);
        this.serviceId = Objects.requireNonNull(serviceId);
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters,
            PrintWriter output) throws Exception {

        if (!parameters.containsKey("enable")) {
            LOGGER.error("required \"enable\" parameter not found in request");
            return;
        }

        final boolean enable = Boolean
                .valueOf(parameters.get("enable").asList().get(0));
        final String reason;
        if (parameters.containsKey("reason")) {
            reason = parameters.get("reason").asList().get(0);
        } else {
            reason = null;
        }

        try {
            consul.agentClient().toggleMaintenanceMode(serviceId, enable,
                    reason);
        } catch (ConsulException e) {
            LOGGER.warn(String.format(
                    "Unable to toggle maintenance mode for service %s",
                    serviceId), e);
            return;
        }

        final String message;
        if (enable) {
            if (!Strings.isNullOrEmpty(reason)) {
                message = String.format(
                        "Enabling maintenance mode for service %s (reason: %s)",
                        serviceId, reason);
            } else {
                message = String.format(
                        "Enabling maintenance mode for service %s (no reason given)",
                        serviceId);
            }
        } else {
            message = String.format("Disabling maintenance mode for service %s",
                    serviceId);
        }
        LOGGER.warn(message);
        output.println(message);
        output.flush();
    }
}

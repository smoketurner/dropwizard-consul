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
package com.smoketurner.dropwizard.consul.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.orbitz.consul.Consul;
import com.orbitz.consul.NotRegisteredException;

public class ConsulServiceCheckTask extends AbstractLifeCycle.AbstractLifeCycleListener implements Runnable  {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulServiceCheckTask.class);
    private final Consul consul;
    private final String serviceId;
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Constructor
     *
     * @param consul
     *            Consul client
     * @param serviceId
     *            Service ID
     */
    public ConsulServiceCheckTask(@Nonnull final Consul consul,
            @Nonnull final String serviceId) {
        this.consul = Objects.requireNonNull(consul);
        this.serviceId = Objects.requireNonNull(serviceId);
    }

    @Override
    public void run() {
        if (!started.get()) {
            LOGGER.trace("Waiting for service start for ID: {}", serviceId);
            return;
        }

        LOGGER.trace("Resetting service TTL check for ID: {}", serviceId);
        try {
            consul.agentClient().pass(serviceId);
        } catch (NotRegisteredException e) {
            LOGGER.warn(
                    "Service ID (" + serviceId + ") not registered with Consul",
                    e);
        } catch (Exception e) {
            LOGGER.warn("Unable to query Consul", e);
        }
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        started.set(true);
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        started.set(false);
    }
}

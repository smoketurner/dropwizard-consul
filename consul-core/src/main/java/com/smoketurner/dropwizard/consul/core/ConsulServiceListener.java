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
package com.smoketurner.dropwizard.consul.core;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.jetty.server.Server;
import io.dropwizard.lifecycle.ServerLifecycleListener;

public class ConsulServiceListener implements ServerLifecycleListener {

    private final ConsulAdvertiser advertiser;

    /**
     * Constructor
     *
     * @param advertiser
     *            Consul advertiser
     */
    public ConsulServiceListener(@Nonnull final ConsulAdvertiser advertiser) {
        this.advertiser = Objects.requireNonNull(advertiser);
    }

    @Override
    public void serverStarted(final Server server) {
        final int applicationPort = getLocalPort(server);
        final int adminPort = getAdminPort(server);
        advertiser.register(applicationPort, adminPort);
    }
}

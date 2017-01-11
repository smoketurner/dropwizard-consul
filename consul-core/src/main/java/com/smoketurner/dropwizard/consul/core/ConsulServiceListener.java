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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dropwizard.lifecycle.ServerLifecycleListener;

public class ConsulServiceListener implements ServerLifecycleListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulServiceListener.class);

    private static final String APPLICATION_CONNECTOR_NAME = "application";
    private static final String ADMIN_CONNECTOR_NAME = "admin";

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
        // Detect the port Jetty is listening on
        try {
            int applicationPort = -1;
            int adminPort = -1;
            for (Connector connector : server.getConnectors()) {
                @SuppressWarnings("resource")
                final ServerConnector serverConnector = (ServerConnector) connector;
                final int port = serverConnector.getLocalPort();
                switch (serverConnector.getName()) {
                case APPLICATION_CONNECTOR_NAME:
                    applicationPort = port;
                    break;
                case ADMIN_CONNECTOR_NAME:
                    adminPort = port;
                    break;
                default:
                    // if we are here, then the server is a "simple" type
                    // which means both contexts are running on the same port
                    applicationPort = port;
                    adminPort = port;
                    break;
                }
            }
            if (applicationPort < 0) {
                throw new IllegalStateException(
                        "Unable to get ports to register with Consul");
            }
            if (adminPort < 0) {
                adminPort = applicationPort;
            }
            advertiser.register(applicationPort, adminPort);
        } catch (Exception e) {
            LOGGER.error("Unable to get listening port", e);
        }
    }
}

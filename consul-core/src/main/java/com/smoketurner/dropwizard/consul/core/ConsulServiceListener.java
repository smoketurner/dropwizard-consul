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

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dropwizard.lifecycle.ServerLifecycleListener;

public class ConsulServiceListener implements ServerLifecycleListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulServiceListener.class);
    private static final String APPLICATION_CONNECTOR = "application";
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
    public void serverStarted(Server server) {
        // Detect the port Jetty is listening on
        for (final Connector connector : server.getConnectors()) {
            if (APPLICATION_CONNECTOR.equals(connector.getName())) {
                final ServerSocketChannel channel = (ServerSocketChannel) connector
                        .getTransport();

                try {
                    final InetSocketAddress socket = (InetSocketAddress) channel
                            .getLocalAddress();
                    advertiser.initialize(socket.getHostString(),
                            socket.getPort());
                    advertiser.register();
                    return;
                } catch (final Exception e) {
                    LOGGER.error("Unable to register service in Consul", e);
                }
            }
        }
    }
}

/**
 * Copyright 2018 Smoke Turner, LLC.
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
package com.smoketurner.dropwizard.consul;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.smoketurner.dropwizard.consul.config.ConsulSubstitutor;
import com.smoketurner.dropwizard.consul.core.ConsulAdvertiser;
import com.smoketurner.dropwizard.consul.core.ConsulServiceListener;
import com.smoketurner.dropwizard.consul.health.ConsulHealthCheck;
import com.smoketurner.dropwizard.consul.managed.ConsulAdvertiserManager;
import com.smoketurner.dropwizard.consul.task.MaintenanceTask;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Replace variables with values from Consul KV. By default, this only works
 * with a Consul agent running on localhost:8500 (the default) as there's no way
 * to configure Consul in the initialize methods. You may override
 * {@link #getConsulAgentHost()} and {@link #getConsulAgentPort()} to provide
 * other defaults.
 *
 * @param <C>
 *            The configuration class for your Dropwizard Application.
 */
public abstract class ConsulBundle<C extends Configuration>
        implements ConfiguredBundle<C>, ConsulConfiguration<C> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulBundle.class);
    private final String defaultServiceName;
    private final boolean strict;
    private final boolean substitutionInVariables;

    static final String CONSUL_AUTH_HEADER_KEY = "X-Consul-Token";

    /**
     * Constructor
     *
     * @param name
     *            Service Name
     */
    public ConsulBundle(@Nonnull final String name) {
        this(name, false);
    }

    /**
     *
     * @param name
     *            Service Name
     * @param strict
     *            If true, the application fails fast if a key cannot be found
     *            in Consul KV
     */
    public ConsulBundle(@Nonnull final String name, final boolean strict) {
        this(name, strict, false);
    }

    /**
     *
     * @param name
     *            Service Name
     * @param strict
     *            If true, the application fails fast if a key cannot be found
     *            in Consul KV
     * @param substitutionInVariables
     *            If true, substitution will be done within variable names.
     */
    public ConsulBundle(@Nonnull final String name, final boolean strict,
            final boolean substitutionInVariables) {
        this.defaultServiceName = Objects.requireNonNull(name);
        this.strict = strict;
        this.substitutionInVariables = substitutionInVariables;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // Replace variables with values from Consul KV. Please override
        // getConsulAgentHost() and getConsulAgentPort() if Consul is not
        // listening on the default localhost:8500.
        try {
            LOGGER.debug("Building Consul Client");
            Consul.Builder builder = Consul.builder()
                .withHostAndPort(HostAndPort
                .fromParts(getConsulAgentHost(), getConsulAgentPort()));

            if(getConsulAclToken().isPresent()) {
                // setting both acl token here and with header, supplying an auth header. This should
                // cover both use cases - endpoint supports legacy ?token query param and other case
                // in which endpoint requires an X-Consul-Token header.
                // see: https://www.consul.io/api/index.html#acls

                LOGGER.debug("Building Consul Client with ACL Support");
                LOGGER.debug("Consul Agent Host: {}", getConsulAgentHost());
                LOGGER.debug("Consul Agent Port: {}", getConsulAgentPort());
                LOGGER.debug("Consul Agent ACL: {}", getConsulAclToken());

                Map<String, String> headers = new HashMap<>();
                headers.put(CONSUL_AUTH_HEADER_KEY, getConsulAclToken().get());

                builder.withAclToken(getConsulAclToken().get())
                    .withHeaders(headers);
            }

            bootstrap.setConfigurationSourceProvider(
                    new SubstitutingSourceProvider(
                            bootstrap.getConfigurationSourceProvider(),
                            new ConsulSubstitutor(builder.build(), strict,
                                    substitutionInVariables)));
        } catch (ConsulException e) {
            LOGGER.warn(
                    "Unable to query Consul running on {}:{},"
                            + " disabling configuration substitution",
                    getConsulAgentHost(), getConsulAgentPort(), e);
        }
    }

    @Override
    public void run(C configuration, Environment environment) throws Exception {
        final ConsulFactory consulConfig = getConsulFactory(configuration);
        if (!consulConfig.isEnabled()) {
            LOGGER.warn("Consul bundle disabled.");
        } else {
            runEnabled(consulConfig, environment);
        }
    }

    protected void runEnabled(ConsulFactory consulConfig,
            Environment environment) {
        if (isNullOrEmpty(consulConfig.getServiceName())){
            consulConfig.setSeviceName(defaultServiceName);
        }
        setupEnvironment(consulConfig, environment);
    }

    protected void setupEnvironment(ConsulFactory consulConfig, Environment environment) {

        final Consul consul = consulConfig.build();
        final String serviceId = getConsulServiceId();
        final ConsulAdvertiser advertiser = new ConsulAdvertiser(environment,
                consulConfig, consul, serviceId);

        // Register a Jetty listener to get the listening host and port
        environment.lifecycle().addServerLifecycleListener(
                new ConsulServiceListener(advertiser));

        // Register a ping healthcheck to the Consul agent
        environment.healthChecks().register("consul",
                new ConsulHealthCheck(consul));

        // Register a shutdown manager to deregister the service
        environment.lifecycle().manage(new ConsulAdvertiserManager(advertiser));

        // Add an administrative task to toggle maintenance mode
        environment.admin().addTask(new MaintenanceTask(consul, serviceId));
    }

    /**
     * Override as necessary to provide an alternative Consul Agent Host
     *
     * @return By default, "localhost"
     */
    @VisibleForTesting
    public String getConsulAgentHost() {
        return Consul.DEFAULT_HTTP_HOST;
    }

    /**
     * Override as necessary to provide an alternative Consul Agent Port
     *
     * @return By default, 8500
     */
    @VisibleForTesting
    public int getConsulAgentPort() {
        return Consul.DEFAULT_HTTP_PORT;
    }

    /**
     * Override as necessary to provide an alternative service ID
     *
     * @return By default, a random UUID v4
     */
    @VisibleForTesting
    public String getConsulServiceId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Override as necessary to provide an alternative Acl Token - if needed.
     *
     * @return By default - empty string (Consul created with out acl support)
     */
    @VisibleForTesting
    public Optional<String> getConsulAclToken() {
        return Optional.empty();
    }
}

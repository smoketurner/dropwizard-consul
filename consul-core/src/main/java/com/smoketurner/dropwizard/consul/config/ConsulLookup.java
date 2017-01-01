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
package com.smoketurner.dropwizard.consul.config;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.text.StrLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.orbitz.consul.Consul;
import io.dropwizard.configuration.UndefinedEnvironmentVariableException;

/**
 * A custom {@link org.apache.commons.lang3.text.StrLookup} implementation using
 * Consul KV as lookup source.
 */
public class ConsulLookup extends StrLookup<Object> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConsulLookup.class);
    private final boolean strict;
    private final Consul consul;

    /**
     * Create a new instance with strict behavior.
     * 
     * @param consul
     *            Consul client
     */
    public ConsulLookup(@Nonnull final Consul consul) {
        this(consul, true);
    }

    /**
     * Constructor
     *
     * @param consul
     *            Consul client
     * @param strict
     *            {@code true} if looking up undefined environment variables
     *            should throw a {@link UndefinedEnvironmentVariableException},
     *            {@code false} otherwise.
     * @throws UndefinedEnvironmentVariableException
     *             if the environment variable doesn't exist and strict behavior
     *             is enabled.
     */
    public ConsulLookup(@Nonnull final Consul consul, final boolean strict) {
        this.consul = Objects.requireNonNull(consul);
        this.strict = strict;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UndefinedEnvironmentVariableException
     *             if the environment variable doesn't exist and strict behavior
     *             is enabled.
     */
    @Override
    public String lookup(String key) {
        try {
            final Optional<String> value = consul.keyValueClient()
                    .getValueAsString(key);
            if (value.isPresent()) {
                return value.get();
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to lookup key in consul", e);
        }

        if (strict) {
            throw new UndefinedEnvironmentVariableException(String.format(
                    "The variable with key '%s' is not found in the Consul KV store;"
                            + " could not substitute the expression '${%s}'.",
                    key, key));
        }
        return null;
    }
}

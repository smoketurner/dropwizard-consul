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
package com.smoketurner.dropwizard.consul.config;

import com.orbitz.consul.Consul;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.UndefinedEnvironmentVariableException;

/** A custom {@link StrSubstitutor} using Consul KV as lookup source. */
public class ConsulSubstitutor extends EnvironmentVariableSubstitutor {

  public ConsulSubstitutor(final Consul consul) {
    this(consul, true, false);
  }

  public ConsulSubstitutor(final Consul consul, boolean strict) {
    this(consul, strict, false);
  }

  /**
   * Constructor
   *
   * @param consul Consul client
   * @param strict {@code true} if looking up undefined environment variables should throw a {@link
   *     UndefinedEnvironmentVariableException}, {@code false} otherwise.
   * @param substitutionInVariables a flag whether substitution is done in variable names.
   * @see io.dropwizard.configuration.EnvironmentVariableLookup#EnvironmentVariableLookup(boolean)
   * @see org.apache.commons.lang3.text.StrSubstitutor#setEnableSubstitutionInVariables(boolean)
   */
  public ConsulSubstitutor(final Consul consul, boolean strict, boolean substitutionInVariables) {
    super(strict);
    this.setVariableResolver(new ConsulLookup(consul, strict));
    this.setEnableSubstitutionInVariables(substitutionInVariables);
  }
}

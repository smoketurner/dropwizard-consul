/*
 * Copyright © 2018 Smoke Turner, LLC (github@smoketurner.com)
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
package com.smoketurner.dropwizard.consul.ribbon;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;
import java.util.Collection;
import java.util.Objects;

public class HealthyConsulServiceDiscoverer implements ConsulServiceDiscoverer {

  private final String service;

  /**
   * Constructor
   *
   * @param service Service name
   */
  public HealthyConsulServiceDiscoverer(final String service) {
    this.service = Objects.requireNonNull(service);
  }

  @Override
  public Collection<ServiceHealth> discover(final Consul consul) {
    return consul.healthClient().getHealthyServiceInstances(service).getResponse();
  }
}

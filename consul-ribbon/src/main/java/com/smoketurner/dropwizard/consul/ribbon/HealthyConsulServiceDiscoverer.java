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
package com.smoketurner.dropwizard.consul.ribbon;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;

public class HealthyConsulServiceDiscoverer implements ConsulServiceDiscoverer {

    private final String serviceName;

    /**
     * Constructor
     *
     * @param serviceName
     *            Service name
     */
    public HealthyConsulServiceDiscoverer(@Nonnull final String serviceName) {
        this.serviceName = Objects.requireNonNull(serviceName);
    }

    @Override
    public Collection<ServiceHealth> discover(@Nonnull final Consul consul) {
        return consul.healthClient().getHealthyServiceInstances(serviceName)
                .getResponse();
    }
}

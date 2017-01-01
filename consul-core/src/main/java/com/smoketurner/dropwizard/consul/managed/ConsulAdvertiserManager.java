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
package com.smoketurner.dropwizard.consul.managed;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.smoketurner.dropwizard.consul.core.ConsulAdvertiser;
import io.dropwizard.lifecycle.Managed;

public class ConsulAdvertiserManager implements Managed {

    private final ConsulAdvertiser advertiser;

    /**
     * Constructor
     *
     * @param advertiser
     *            Consul advertiser
     */
    public ConsulAdvertiserManager(@Nonnull final ConsulAdvertiser advertiser) {
        this.advertiser = Objects.requireNonNull(advertiser);
    }

    @Override
    public void start() throws Exception {
        // the advertiser is register as a Jetty startup listener
    }

    @Override
    public void stop() throws Exception {
        advertiser.deregister();
    }
}

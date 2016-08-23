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

import com.google.common.primitives.Ints;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.WeightedResponseTimeRule;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

import javax.annotation.Nonnull;
import java.util.Objects;

public class RibbonLoadBalancerBuilder {

    private final ConsulServerList consulServerList;

    /**
     * Constructor
     *
     * @param consulServerList
     *            Consul server list consulServerList
     */
    public RibbonLoadBalancerBuilder(
            @Nonnull final ConsulServerList consulServerList) {
        this.consulServerList = Objects.requireNonNull(consulServerList);
    }

    /**
     * Build a new {@link ZoneAwareLoadBalancer}
     *
     * @param configuration
     *            load balancer configuration
     * @return a new ZoneAwareLoadBalancer
     */
    public ZoneAwareLoadBalancer<Server> build(
            @Nonnull final RibbonLoadBalancerConfiguration configuration) {
        final DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
        clientConfig.setClientName(consulServerList.getName());
        clientConfig.set(CommonClientConfigKey.ServerListRefreshInterval,
                Ints.checkedCast(
                        configuration.getRefreshInterval().toMilliseconds()));
        return LoadBalancerBuilder.newBuilder().withClientConfig(clientConfig)
                .withRule(new WeightedResponseTimeRule())
                .withDynamicServerList(consulServerList)
                .buildDynamicServerListLoadBalancer();
    }
}

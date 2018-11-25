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
package com.smoketurner.dropwizard.consul.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck.Result;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import org.junit.Before;
import org.junit.Test;

public class ConsulHealthCheckTest {

  private final Consul consul = mock(Consul.class);
  private final AgentClient agent = mock(AgentClient.class);
  private final ConsulHealthCheck healthCheck = new ConsulHealthCheck(consul);

  @Before
  public void setUp() {
    when(consul.agentClient()).thenReturn(agent);
  }

  @Test
  public void testCheckHealthy() throws Exception {
    final Result actual = healthCheck.check();
    verify(agent).ping();
    assertThat(actual.isHealthy()).isTrue();
  }

  @Test
  public void testCheckUnhealthy() throws Exception {
    doThrow(new ConsulException("error")).when(agent).ping();
    final Result actual = healthCheck.check();
    verify(agent).ping();
    assertThat(actual.isHealthy()).isFalse();
  }
}

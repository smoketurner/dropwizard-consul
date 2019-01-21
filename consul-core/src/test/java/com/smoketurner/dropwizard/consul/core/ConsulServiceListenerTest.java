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
package com.smoketurner.dropwizard.consul.core;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orbitz.consul.ConsulException;
import io.dropwizard.util.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsulServiceListenerTest {

  private final ConsulAdvertiser advertiser = mock(ConsulAdvertiser.class);
  private ScheduledExecutorService scheduler;

  @Before
  public void setUp() {
    scheduler = Executors.newScheduledThreadPool(1);
  }

  @After
  public void tearDown() {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }

  @Test
  public void testRegister() {
    final ConsulServiceListener listener =
        new ConsulServiceListener(
            advertiser, Optional.of(Duration.milliseconds(1)), Optional.of(scheduler));

    when(advertiser.register(anyInt(), anyInt()))
        .thenThrow(new ConsulException("Cannot connect to Consul"))
        .thenReturn(true);

    listener.register(0, 0);

    verify(advertiser, timeout(100).atLeast(1)).register(0, 0);
  }
}

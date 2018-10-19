/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orbitz.consul.ConsulException;
import io.dropwizard.util.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;

public class ConsulServiceListenerTest {

  private final ConsulAdvertiser advertiser = mock(ConsulAdvertiser.class);

  @Test
  public void testRegister() {
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    final ConsulServiceListener listener =
        new ConsulServiceListener(
            advertiser, Optional.of(Duration.seconds(5)), Optional.of(scheduler));

    doThrow(new ConsulException("Cannot connect to Consul"))
        .when(advertiser)
        .register(anyInt(), anyInt());

    listener.register(100, 200);
    doNothing().when(advertiser).register(anyInt(), anyInt());

    try {
      Thread.sleep(10_000L);
    } catch (InterruptedException ignore) {
    }
    verify(advertiser, times(2)).register(100, 200);
  }
}

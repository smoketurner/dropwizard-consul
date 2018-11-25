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
package com.smoketurner.dropwizard.consul.managed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.smoketurner.dropwizard.consul.core.ConsulAdvertiser;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;

public class ConsulAdvertiserManagerTest {

  private final ConsulAdvertiser advertiser = mock(ConsulAdvertiser.class);
  private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
  private final ConsulAdvertiserManager manager =
      new ConsulAdvertiserManager(advertiser, Optional.of(scheduler));

  @Test
  public void testStop() throws Exception {
    manager.stop();
    verify(advertiser).deregister();
    verify(scheduler).shutdownNow();
  }
}

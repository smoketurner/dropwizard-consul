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
package com.smoketurner.dropwizard.consul.core;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;

public class ConsulServiceCheckTaskTest {

    private static final String SERVICE_ID = "test";
    private final Consul consul = mock(Consul.class);
    private final AgentClient agent = mock(AgentClient.class);
    private final ConsulServiceCheckTask task = new ConsulServiceCheckTask(
            consul, SERVICE_ID);

    @Before
    public void setUp() {
        when(consul.agentClient()).thenReturn(agent);
    }

    @Test
    public void testRun() throws Exception {
        task.lifeCycleStarted(null);
        task.run();
        verify(agent).pass(SERVICE_ID);
    }

    @Test
    public void testPreLifecycleStartedEvent() throws Exception {
        task.run();
        verify(agent, never()).pass(anyString());
    }

    @Test
    public void testPostLifecycleStoppingEvent() throws Exception {
        testRun();
        task.lifeCycleStopping(null);
        reset(agent);
        testPreLifecycleStartedEvent();
    }
}

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
package com.smoketurner.dropwizard.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

public class ConsulBundleTest {

    private final ConsulFactory factory = spy(new ConsulFactory());
    private final Environment environment = mock(Environment.class);
    private final TestConfiguration config = new TestConfiguration();
    private ConsulBundle<TestConfiguration> bundle;

    class TestConfiguration extends Configuration
            implements ConsulConfiguration<TestConfiguration> {
        @Override
        public ConsulFactory getConsulFactory(TestConfiguration configuration) {
            return factory;
        }
    }

    @Before
    public void setUp() throws Exception {
        bundle = spy(new ConsulBundle<TestConfiguration>("test") {
            @Override
            public ConsulFactory getConsulFactory(TestConfiguration c) {
                return c.getConsulFactory(c);
            }
        });

        doNothing().when(bundle).runEnabled(factory, environment);
    }

    @Test
    public void testDefaultsToEnabled() throws Exception {
        assertThat(factory.isEnabled()).isTrue();
    }

    @Test
    public void testEnabled() throws Exception {
        doReturn(true).when(factory).isEnabled();
        bundle.run(config, environment);
        verify(bundle, times(1)).runEnabled(factory, environment);
    }

    @Test
    public void testNotEnabled() throws Exception {
        doReturn(false).when(factory).isEnabled();
        bundle.run(config, environment);
        verify(bundle, times(0)).runEnabled(factory, environment);
    }
}

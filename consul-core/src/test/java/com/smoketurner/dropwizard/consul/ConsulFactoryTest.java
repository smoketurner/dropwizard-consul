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
package com.smoketurner.dropwizard.consul;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import io.dropwizard.util.Duration;
import org.junit.Test;

public class ConsulFactoryTest {

  @Test
  public void testEquality() {
    final ConsulFactory actual = createFullyPopulatedConsulFactory();
    final ConsulFactory expected = createFullyPopulatedConsulFactory();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testNotEqual() {
    final ConsulFactory actual = createFullyPopulatedConsulFactory();
    final ConsulFactory expected = createFullyPopulatedConsulFactory();
    expected.setAdminPort(200);
    assertThat(actual).isNotEqualTo((expected));
  }

  @Test
  public void testHashCode() {
    final ConsulFactory actual = createFullyPopulatedConsulFactory();
    final ConsulFactory expected = createFullyPopulatedConsulFactory();
    assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
  }

  @Test
  public void testMutatedHashCode() {
    final ConsulFactory actual = createFullyPopulatedConsulFactory();
    final ConsulFactory expected = createFullyPopulatedConsulFactory();
    expected.setAdminPort(200);
    assertThat(actual.hashCode()).isNotEqualTo(expected.hashCode());
  }

  private ConsulFactory createFullyPopulatedConsulFactory() {
    final ConsulFactory consulFactory = new ConsulFactory();
    consulFactory.setSeviceName("serviceName");
    consulFactory.setEnabled(true);
    consulFactory.setServicePort(1000);
    consulFactory.setAdminPort(2000);
    consulFactory.setServiceAddress("localhost");
    consulFactory.setTags(ImmutableList.of("tag1", "tag2"));
    consulFactory.setRetryInterval(Duration.seconds(5));
    consulFactory.setCheckInterval(Duration.seconds(1));
    consulFactory.setAclToken("acl-token");
    consulFactory.setServicePing(false);
    return consulFactory;
  }
}

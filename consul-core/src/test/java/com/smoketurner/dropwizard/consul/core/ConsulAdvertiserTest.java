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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.setup.Environment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;

public class ConsulAdvertiserTest {

  public static final String SECOND_SUBNET_IP = "192.168.2.99";
  public static final String FIRST_SUBNET_IP = "192.168.1.53";
  public static final String THIRD_SUBNET_IP = "192.168.3.32";
  private static final String DEFAULT_HEALTH_CHECK_PATH = "ping";
  private final Consul consul = mock(Consul.class);
  private final AgentClient agent = mock(AgentClient.class);
  private final Environment environment = mock(Environment.class);
  private final MutableServletContextHandler handler = mock(MutableServletContextHandler.class);
  private final Supplier<String> supplierMock = mock(Supplier.class);
  private final String serviceId = "test";
  private ConsulAdvertiser advertiser;
  private ConsulFactory factory;
  private String healthCheckUrl;

  @Before
  public void setUp() {
    when(consul.agentClient()).thenReturn(agent);
    when(environment.getAdminContext()).thenReturn(handler);
    when(handler.getContextPath()).thenReturn("admin");
    when(supplierMock.get()).thenReturn(null);
    factory = new ConsulFactory();
    factory.setSeviceName("test");
    factory.setServiceSubnet("192.168.2.0/24");
    factory.setServiceAddressSupplier(supplierMock);
    factory.setHealthCheckPath(DEFAULT_HEALTH_CHECK_PATH);
    advertiser = new ConsulAdvertiser(environment, factory, consul, serviceId);
    healthCheckUrl = "http://127.0.0.1:8081/admin/" + DEFAULT_HEALTH_CHECK_PATH;
  }

  @Test
  public void testGetServiceId() {
    assertThat(advertiser.getServiceId()).isEqualTo(serviceId);
  }

  @Test
  public void testRegister() {
    when(agent.isRegistered(serviceId)).thenReturn(false);
    advertiser.register("http", 8080, 8081);

    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .port(8080)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "http"))
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testRegisterWithSubnet() {
    when(agent.isRegistered(serviceId)).thenReturn(false);
    advertiser.register(
        "http", 8080, 8081, Arrays.asList(FIRST_SUBNET_IP, SECOND_SUBNET_IP, THIRD_SUBNET_IP));

      String healthCheckUrlWithCorrectSubnet = "http://192.168.2.99:8081/admin/" + DEFAULT_HEALTH_CHECK_PATH;
      final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .port(8080)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrlWithCorrectSubnet)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .address(SECOND_SUBNET_IP)
            .meta(ImmutableMap.of("scheme", "http"))
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testRegisterWithSubnetNoEligibleIps() {
    when(agent.isRegistered(serviceId)).thenReturn(false);
    advertiser.register(
        "http", 8080, 8081, Arrays.asList(FIRST_SUBNET_IP, "192.168.7.23", THIRD_SUBNET_IP));

    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .port(8080)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "http"))
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testRegisterWithSupplier() {
    when(agent.isRegistered(serviceId)).thenReturn(false);
    when(supplierMock.get()).thenReturn("192.168.8.99");
    advertiser.register(
        "http", 8080, 8081, Arrays.asList(FIRST_SUBNET_IP, "192.168.7.23", THIRD_SUBNET_IP));

      String healthCheckUrlWithCorrectSubnet = "http://192.168.8.99:8081/admin/ping";
      final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .port(8080)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrlWithCorrectSubnet)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "http"))
            .address("192.168.8.99")
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testRegisterWithSupplierException() {
    when(agent.isRegistered(serviceId)).thenReturn(false);
    when(supplierMock.get()).thenThrow(new IllegalArgumentException());
    advertiser.register(
        "http", 8080, 8081, Arrays.asList(FIRST_SUBNET_IP, "192.168.7.23", THIRD_SUBNET_IP));

    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .port(8080)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "http"))
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testRegisterWithHttps() {
    when(agent.isRegistered(serviceId)).thenReturn(false);
    advertiser.register("https", 8080, 8081);

    String httpsHealthCheckUrl = "https://127.0.0.1:8081/admin/ping";
    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .port(8080)
            .check(
                ImmutableRegCheck.builder()
                    .http(httpsHealthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "https"))
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testRegisterAlreadyRegistered() {
    when(agent.isRegistered(anyString())).thenReturn(true);
    advertiser.register("http", 8080, 8081);
    verify(agent, never())
        .register(anyInt(), anyString(), anyLong(), anyString(), anyString(), anyList(), anyMap());
  }

  @Test
  public void testHostFromConfig() {
    factory.setServicePort(8888);
    factory.setServiceAddress("127.0.0.1");

    when(agent.isRegistered(anyString())).thenReturn(false);
    final ConsulAdvertiser advertiser =
        new ConsulAdvertiser(environment, factory, consul, serviceId);
    advertiser.register("http", 8080, 8081);

    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .id(serviceId)
            .port(8888)
            .address("127.0.0.1")
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "http"))
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testTagsFromConfig() {
    final List<String> tags = Arrays.asList("test", "second-test");
    factory.setTags(tags);

    when(agent.isRegistered(serviceId)).thenReturn(false);
    final ConsulAdvertiser advertiser =
        new ConsulAdvertiser(environment, factory, consul, serviceId);
    advertiser.register("http", 8080, 8081);

      final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .tags(tags)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .meta(ImmutableMap.of("scheme", "http"))
            .port(8080)
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testAclTokenFromConfig() {
    String aclToken = "acl-token";
    factory.setAclToken(aclToken);

    when(agent.isRegistered(serviceId)).thenReturn(false);
    final ConsulAdvertiser advertiser =
        new ConsulAdvertiser(environment, factory, consul, serviceId);
    advertiser.register("http", 8080, 8081);

    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .id(serviceId)
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .port(8080)
            .meta(ImmutableMap.of("scheme", "http"))
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testServiceMetaFromConfig() {
    final Map<String, String> serviceMeta = new HashMap<>();
    serviceMeta.put("meta1-key", "meta1-value");
    serviceMeta.put("meta2-key", "meta2-value");
    factory.setServiceMeta(serviceMeta);

    when(agent.isRegistered(serviceId)).thenReturn(false);
    final ConsulAdvertiser advertiser =
        new ConsulAdvertiser(environment, factory, consul, serviceId);
    advertiser.register("http", 8080, 8081);

    final ImmutableRegistration registration =
        ImmutableRegistration.builder()
            .meta(serviceMeta)
            .putMeta("scheme", "http")
            .check(
                ImmutableRegCheck.builder()
                    .http(healthCheckUrl)
                    .interval("1s")
                    .deregisterCriticalServiceAfter("1m")
                    .build())
            .name("test")
            .port(8080)
            .id(serviceId)
            .build();

    verify(agent).register(registration);
  }

  @Test
  public void testDeregister() {
    final String serviceId = advertiser.getServiceId();
    when(agent.isRegistered(serviceId)).thenReturn(true);
    advertiser.deregister();
    verify(agent).deregister(serviceId);
  }

  @Test
  public void testDeregisterNotRegistered() {
    final String serviceId = advertiser.getServiceId();
    when(agent.isRegistered(serviceId)).thenReturn(false);
    advertiser.deregister();
    verify(agent, never()).deregister(serviceId);
  }

  @Test
  public void testDeregisterException() {
    when(agent.isRegistered(anyString())).thenReturn(true);
    doThrow(new ConsulException("error")).when(agent).deregister(anyString());
    advertiser.deregister();
    verify(agent).deregister(anyString());
  }
}

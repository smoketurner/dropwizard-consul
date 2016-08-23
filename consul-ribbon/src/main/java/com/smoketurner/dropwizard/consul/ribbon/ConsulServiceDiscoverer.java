package com.smoketurner.dropwizard.consul.ribbon;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;

import java.util.Collection;

@FunctionalInterface
public interface ConsulServiceDiscoverer {
    Collection<ServiceHealth> discover(Consul consul);
}

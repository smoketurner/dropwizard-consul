package com.smoketurner.dropwizard.consul.ribbon;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;

import java.util.Collection;

public class HealthyConsulServiceDiscoverer implements ConsulServiceDiscoverer {

    private final String serviceName;

    public HealthyConsulServiceDiscoverer(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public Collection<ServiceHealth> discover(Consul consul) {
        return consul.healthClient().getHealthyServiceInstances(serviceName).getResponse();
    }
}

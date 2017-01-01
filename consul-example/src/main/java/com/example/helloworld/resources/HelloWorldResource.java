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
package com.example.helloworld.resources;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.codahale.metrics.annotation.Timed;
import com.example.helloworld.api.Saying;
import com.google.common.base.Optional;
import com.netflix.loadbalancer.Server;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.ServiceHealth;
import com.smoketurner.dropwizard.consul.ribbon.RibbonJerseyClient;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
    private final Consul consul;
    private final RibbonJerseyClient client;
    private final String template;
    private final String defaultName;
    private final AtomicLong counter;

    public HelloWorldResource(Consul consul, RibbonJerseyClient client,
            String template, String defaultName) {
        this.consul = consul;
        this.client = client;
        this.template = template;
        this.defaultName = defaultName;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    @Path("/hello-world")
    public Saying sayHello(@QueryParam("name") Optional<String> name) {
        final String value = String.format(template, name.or(defaultName));
        return new Saying(counter.incrementAndGet(), value);
    }

    @GET
    @Timed
    @Path("/consul/{service}")
    public List<ServiceHealth> getHealthyServiceInstances(
            @PathParam("service") String service) {
        return consul.healthClient().getHealthyServiceInstances(service)
                .getResponse();
    }

    @GET
    @Timed
    @Path("/available")
    public List<Server> getAvailableServers() {
        return client.getAvailableServers();
    }
}

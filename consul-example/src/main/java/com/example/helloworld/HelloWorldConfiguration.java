package com.example.helloworld;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import com.smoketurner.dropwizard.consul.ribbon.RibbonLoadBalancerConfiguration;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class HelloWorldConfiguration extends Configuration {

    @NotEmpty
    private String template = "Hello, %s!";

    @NotEmpty
    private String defaultName = "Stranger";

    @NotNull
    @Valid
    public final ConsulFactory consul = new ConsulFactory();

    @NotNull
    @Valid
    public final RibbonLoadBalancerConfiguration downstream = new RibbonLoadBalancerConfiguration();

    @JsonProperty
    public String getTemplate() {
        return template;
    }

    @JsonProperty
    public void setTemplate(String template) {
        this.template = template;
    }

    @JsonProperty
    public String getDefaultName() {
        return defaultName;
    }

    @JsonProperty
    public void setDefaultName(String name) {
        this.defaultName = name;
    }

    @JsonProperty
    public ConsulFactory getConsulFactory() {
        return consul;
    }

    @JsonProperty
    public RibbonLoadBalancerConfiguration getDownstream() {
        return downstream;
    }
}

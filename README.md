Dropwizard Consul Bundle
========================
[![Build Status](https://travis-ci.org/smoketurner/dropwizard-consul.svg?branch=master)](https://travis-ci.org/smoketurner/dropwizard-consul)
[![Maven Central](https://img.shields.io/maven-central/v/com.smoketurner.dropwizard/dropwizard-consul.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.smoketurner.dropwizard/dropwizard-consul/)
[![GitHub license](https://img.shields.io/github/license/smoketurner/dropwizard-consul.svg?style=flat-square)](https://github.com/smoketurner/dropwizard-consul/tree/master)
[![Become a Patron](https://img.shields.io/badge/Patron-Patreon-red.svg)](https://www.patreon.com/bePatron?u=9567343)

A bundle for using [Consul](https://consul.io) in Dropwizard applications. Features:

* Integrated client-side load balancer based on [Ribbon](https://github.com/netflix/ribbon)
* Dropwizard health check that monitors reachablility of Consul
* The Dropwizard service is registered as a Consul service with a Consul-side health check querying the Dropwizard [health check](https://www.dropwizard.io/1.3.12/docs/manual/core.html#health-checks)
* Ability to resolve [configuration](https://www.dropwizard.io/1.3.12/docs/manual/core.html#configuration) properties from Consul's KV store
* Admin task to toggle Consul's [maintenance](https://www.consul.io/api/agent.html#enable-maintenance-mode) mode

Dependency Info
---------------
```xml
<dependency>
    <groupId>com.smoketurner.dropwizard</groupId>
    <artifactId>consul-core</artifactId>
    <version>1.3.12-1</version>
</dependency>
<dependency>
    <groupId>com.smoketurner.dropwizard</groupId>
    <artifactId>consul-ribbon</artifactId>
    <version>1.3.12-1</version>
</dependency>
```

Usage
-----
Add a `ConsulBundle` to your [Application](https://www.dropwizard.io/1.3.12/dropwizard-core/apidocs/io/dropwizard/Application.html) class.

```java
@Override
public void initialize(Bootstrap<MyConfiguration> bootstrap) {
    // ...
    bootstrap.addBundle(new ConsulBundle<MyConfiguration>(getName()) {
        @Override
        public ConsulFactory getConsulFactory(MyConfiguration configuration) {
            return configuration.getConsulFactory();
        }
    });
}
```

The bundle also includes a `ConsulSubsitutor` to retrieve configuration values from the Consul KV store. You can define settings in your YAML configuration file:

```
template: ${helloworld/template:-Hello, %s!}
defaultName: ${helloworld/defaultName:-Stranger}
```

The setting with the path `helloworld/template` will be looked up in the KV store and will be replaced in the configuration file when the application is started. You can specify a default value after the `:-`. This currently does not support dynamically updating values in a running Dropwizard application.

Configuration
-------------
For configuring the Consul connection, there is a `ConsulFactory`:

```yaml
consul:
  # Optional properties
  # endpoint for consul (defaults to localhost:8500)
  endpoint: localhost:8500
  # service port
  servicePort: 8080
  # check interval frequency
  checkInterval: 1 second
```

Example Application
-------------------
This bundle includes a modified version of the `HelloWorldApplication` from Dropwizard's [Getting Started](https://www.dropwizard.io/1.3.12/docs/getting-started.html) documentation.

You can execute this application by first starting Consul on your local machine then running:

```
mvn clean package
java -jar consul-example/target/consul-example-1.3.12-2-SNAPSHOT.jar server consul-example/hello-world.yml
```

This will start the application on port `8080` (admin port `8180`). This application demonstrations the following Consul integration points:

- The application is registered as a service with Consul (with the [service port](https://www.consul.io/docs/agent/services.html) set to the applicationConnectors port in the configuration file.
- The application will lookup any variables in the configuration file from Consul upon startup (it defaults to connecting to a Consul agent running on `localhost:8500` for this functionality)
- The application exposes an additional HTTP endpoint for querying Consul for available healthy services:
```
curl -X GET localhost:8080/consul/hello-world -i
HTTP/1.1 200 OK
Date: Mon, 25 Jan 2016 03:42:10 GMT
Content-Type: application/json
Vary: Accept-Encoding
Content-Length: 870

[
    {
        "Node": {
            "Node": "mac",
            "Address": "192.168.1.100",
            "Datacenter": "dc1",
            "TaggedAddresses": {
                "wan": "192.168.1.100",
                "lan": "192.168.1.100"
            },
            "Meta": {
                "consul-network-segment": ""
            }
        },
        "Service": {
            "ID": "test123",
            "Service": "hello-world",
            "EnableTagOverride": false,
            "Tags": [],
            "Address": "",
            "Meta": {
                "scheme": "http"
            },
            "Port": 8080,
            "Weights": {
                "Passing": 1,
                "Warning": 1
            }
        },
        "Checks": [
            {
                "Node": "mac",
                "CheckID": "serfHealth",
                "Name": "Serf Health Status",
                "Status": "passing",
                "Notes": "",
                "Output": "Agent alive and reachable",
                "ServiceID": "",
                "ServiceName": "",
                "ServiceTags": []
            },
            {
                "Node": "mac",
                "CheckID": "service:test123",
                "Name": "Service 'hello-world' check",
                "Status": "passing",
                "Notes": "",
                "Output": "HTTP GET http:\/\/127.0.0.1:8180\/healthcheck: 200 OK Output: {\"consul\":{\"healthy\":true},\"deadlocks\":{\"healthy\":true}}",
                "ServiceID": "test123",
                "ServiceName": "hello-world",
                "ServiceTags": []
            }
        ]
    }
]
```
- The application will periodically checkin with Consul every second to notify the service check that it is still alive
- Upon shutdown, the application will deregister itself from Consul

Credits
-------
This bundle was inspired by an older bundle (Dropwizard 0.6.2) that [Chris Gray](https://github.com/chrisgray) created at https://github.com/chrisgray/dropwizard-consul. I also incorporated the configuration provider changes from https://github.com/remmelt/dropwizard-consul-config-provider

Support
-------
Please file bug reports and feature requests in [GitHub issues](https://github.com/smoketurner/dropwizard-consul/issues).

License
-------
Copyright (c) 2019 Smoke Turner, LLC

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.

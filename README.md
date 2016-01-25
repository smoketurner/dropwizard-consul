Dropwizard Consul Bundle
========================
[![Build Status](https://travis-ci.org/smoketurner/dropwizard-consul.svg?branch=master)](https://travis-ci.org/smoketurner/notification)
[![Coverage Status](https://coveralls.io/repos/smoketurner/dropwizard-consul/badge.svg)](https://coveralls.io/r/smoketurner/notification)
[![Maven Central](https://img.shields.io/maven-central/v/com.smoketurner.dropwizard/dropwizard-consul.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.smoketurner.dropwizard/dropwizard-consul/)
[![GitHub license](https://img.shields.io/github/license/smoketurner/dropwizard-consul.svg?style=flat-square)](https://github.com/smoketurner/dropwizard-consul/tree/master)

A bundle for using [Consul](https://consul.io) in Dropwizard applications.

Dependency Info
---------------
```xml
<dependency>
  <groupId>com.smoketurner.dropwizard</groupId>
  <artifactId>dropwizard-consul</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```


Usage
-----
Add a `ConsulBundle` to your [Application](http://www.dropwizard.io/0.9.2/dropwizard-core/apidocs/io/dropwizard/Application.html) class.

```java
@Override
public void initialize(Bootstrap<MyConfiguration> bootstrap) {
    // ...
    bootstrap.addBundle(new ConsulBundle<MyConfiguration>() {
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
  serviceName: helloworld
```

Example Application
-------------------
This bundle includes a modified version of the `HelloWorldApplication` from Dropwizard's [Getting Started](http://www.dropwizard.io/0.9.2/docs/getting-started.html) documentation. You can execute this application by first starting Consul on your local machine then running:

```
mvn clean package
java -jar target/dropwizard-consul-0.1.0-SNAPSHOT.jar server src/main/resources/hello-world.yml
```

This will start the application on port `8080` (admin port `8180`). This application demonstrations the following Consul integration points:

- The application is registered as a service with Consul (with the [service port](https://www.consul.io/docs/agent/services.html) set to the applicationConnectors port in the configuration file.
- The application will lookup any variables in the configuration file from Consul upon startup (it defaults to connecting to a Consul agent running on `localhost:8500` for this functionality)
- The application exposes an additional HTTP endpoint for querying Consul for available healthy services:
```
curl -X GET localhost:8080/consul/helloworld -i
HTTP/1.1 200 OK
Date: Mon, 25 Jan 2016 03:42:10 GMT
Content-Type: application/json
Vary: Accept-Encoding
Content-Length: 591

[{
    "Node": {
        "Node": "mac",
        "Address": "192.168.1.100"
    },
    "Service": {
        "ID": "e16fc421-f430-4361-9bee-8a700a61c06f",
        "Service": "helloworld",
        "Tags": [],
        "Address": "",
        "Port": 8080
    },
    "Checks": [{
        "Node": "mac",
        "CheckID": "serfHealth",
        "Name": "Serf Health Status",
        "Status": "passing",
        "Notes": "",
        "Output": "Agent alive and reachable",
        "ServiceID": "",
        "ServiceName": ""
    }, {
        "Node": "mac",
        "CheckID": "service:e16fc421-f430-4361-9bee-8a700a61c06f",
        "Name": "Service 'helloworld' check",
        "Status": "passing",
        "Notes": "",
        "Output": "",
        "ServiceID": "e16fc421-f430-4361-9bee-8a700a61c06f",
        "ServiceName": "helloworld"
    }]
}]
```
- The application will periodically checkin with Consul every second to notify the service check that it is still alive
- Upon shutdown, the application will deregister itself from Consul


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/smoketurner/dropwizard-consul/issues).


License
-------

Copyright (c) 2016 Justin Plock

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.

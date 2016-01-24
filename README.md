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


Configuration
-------------
For configuring the Consul connection, there is a `ConsulFactory`:

```yaml
consul:
  # the consul agent's address; required
  endpoint: localhost:8500
```


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/smoketurner/dropwizard-consul/issues).


License
-------

Copyright (c) 2016 Justin Plock

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.

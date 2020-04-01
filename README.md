<iframe width="560" height="315" src="https://www.youtube.com/embed/Hu5DzNogV0E" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>


Hi, Spring fans! Welcome to another installment of Spring Tip! In this installment, we look at something near and dear to my heart (and my `@author` tag!) - the Cloud Foundry Java client auto-configuration. 

## What is Cloud Foundry? 

Cloud Foundry is an open-source  PaaS. It has a lot of flexibility. I'm in love with it if I'm honest. It's simple. I love things like it that give me flexibility without requiring too many sacrifices at the altar of the YAML deity. It's an opinionated platform as a service. You give the platform an application, and it deploys them. You upload a spring boot app, and it figures out that the app is a standalone, self-contained, so-called "fat" `.jar` and it downloads the required JDK, configures the necessary amount of memory and then creates a filesystem with your app, the JDK, the right configuration, and then turns it all into a container. 

Well, to be quite specific, buildpacks are the thing that takes the uploaded artifact and derives for it a runtime and environment to run the application. Buildpacks then get layers on top of the platform's various blessed versions of operating systems. The result is consistency. You get velocity through consistency. You get velocity by knowing that every app looks, _basically_, the same. The platform runs them all with a similar configuration, with some variations allowed for things like RAM, hard disk space, etc. 

The other benefit of this approach is that the platform is responsible for building the container. The entire container creation lifecycle routes through the platform. The platform can create the environment again if it needs to, such as when there are security fixes in the base platform. The platform can transparently rebuild these things to take advantage of the new platform fixes. You can do this too. 

In Cloud Foundry, the act of restaging an application forces it to rebuild that environment. The platform can do this for you automatically, too. You can do rolling restages if you want. You might do this because you want to specify some new environment varaibles. Have a new message queue or database to bind to the application? Restage it. You can do it if you want to upgrade something like the JDK or whatever else is described by the buildpack. Java 14 is out? Great, let's use that. 

Cloud Foundry makes this kind fo work easy because it's all controllable through an HTTP REST API. You can ask the platform, what kind of applications do you have that reusing - say - a Python-based buildpack? Which is using Java? Then you can tell it stage only those.

## The Cloud Foundry Java client

In an [earlier video](https://spring.io/blog/2018/04/11/spring-tips-the-cloud-foundry-java-client), we looked at the cloud foundry java client. The client lets you programmatically (and reactively) talk tot he platform and instruct it do things. In this video, were going to look at the relatively new Cloud Foundry Java client autoconfiguration that's now part of the Spring Cloud Cloud OFundry Discovery Client implementation. We're _not_ going to use the Discovery Client. (that'll have to wait, perhaps, for another vide!)

The Java client is trivial to set up since there's now a Spring Boot autoconfiguration hat does the bulk of the work of connecting to the platform.  Let's build a new project to the [Spring Initalizr](http://start.Spring.io). Choose `Cloud Foundry Discovery Client` and `Lombok` and `Reactive Web`. Click `Generate`.

## Our Sample Applications 

To demonstrate this, we're going to deploy two sample applications, one using the JVM and the other using Python. To keep things simple, we'll use the Spring CLI to create a simple HTTP application using Spring Boot and Groovy. Then We'll create a Python application. 

To deploy these, you will need an account on a Cloud Foundry instance. I'm using [Pivotal Web Services](http://run.pivotal.io). It's cheap to sign up. If you already have an instance, log in to it. 

### The Python Application

Here's the [Python pplication](https://github.com/spring-tips/cloudfoundry-java-client-autoconfiguration/tree/master/sample-apps/python). 

The Python application (`application.py`) is a similarly trial HTTP application using Python's Flask framework. 


```python
#!/usr/bin/env python
from flask import Flask
import os

app = Flask(__name__)
cf_port = os.getenv("PORT")
if cf_port is None:
    cf_port = '5000'

@app.route('/')
def hello():
    return {'message': 'Hello, world'}

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(cf_port), debug=True)

```

[Heroku](http://heroku.com) and [Cloud Foundry](http://cloudfoundry.org) use `Procfile` files to tell the platform that a Python application is meant to be a web application. It describes to the platform how to run the program. 


```yml
web: Python application.py
```

When the platform sees the incoming application, it'll have to download all the artifacts to run the application. You do this with a `Pipfile` file, which is sort of like  Maven's familiar  `pom.xml`.

```Pipfile
[[source]]
name = "pypi"
url = "https://pypi.org/simple"
verify_ssl = true

[dev-packages]

[packages]
flask = "*"

[requires]
python_version = "3.7"
```

Here's the deployment script, `deploy.sh`, that I used to deploy the application. 

```bash
#!/bin/bash

APP_NAME=sample-python-app-${RANDOM}
cf push -p . ${APP_NAME}
```

### The Java Application
Here is the [Java appication](https://github.com/spring-tips/cloudfoundry-java-client-autoconfiguration/tree/master/sample-apps/java).  

The following is a working Spring Boot CLI based application - all of it it. 

```groovy
@RestController 
class GreetingsRestController {

    @GetMapping("/hello")
    String get (){
        return "Hello, world"
    }
}
```

To deploy it, I wrote a little `deploy.sh` with the following incantations. The script is two lines: one line to turn the `.groovy` script into a working Spring Boot (a self-contained "fat" `.jar` with all its dependencies bundled therein) application and another line to deploy it.


```bash
#!/bin/bash 
APP_NAME=hello-world-${RANDOM}
JAR_NAME=app.jar
spring jar  ${JAR_NAME} hello.groovy 
cf push -p ${JAR_NAME} APP_NAME
```

## Restaging with the Cloud Foundry Auto Configuration 

Alright, I wanted to demonstrate how cool it is to be able to just plugin some strings int he property file and then - ta-da! - you can now manipulate your platform with the excellent Cloud Foundry Java client. But there's nothing to see! You just add the Spring boot dependency to the classpath and plugin some properties. So, I will show you the code and configuration to build a selective restage. I've shown you a zillion different videos on building reactive applications; I won't review those basics. Let's just look at some code.

First, here's the configuration properties. I'll leave it t you to specify the values yourselves. In the video above, I have them defined as environment variables (`export SPRING_CLOUD_CLOUNDFOUNDRY_USERNAME=...`). 

```properties
spring.cloud.cloudfoundry.space=joshlong
spring.cloud.cloudfoundry.org=my-org
spring.cloud.cloudfoundry.password=wouldnt-u-like-to-know
spring.cloud.cloudfoundry.username=my-user-email
spring.cloud.cloudfoundry.url=api.run.pivotal.io
```

Now comes the actual Java code.

```java
package com.example.cfac;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class CfacApplication {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(CfacApplication.class, args);
        System.in.read();
    }
}

@Component
@Log4j2
class Restager {

    private final String buildpack = "python";

    private boolean isValidBuildpack(String one, String two) {
        return (StringUtils.hasText(one) ? one : StringUtils.hasText(two) ? two : "").contains(this.buildpack);
    }

    Restager(CloudFoundryOperations ops, CloudFoundryClient client) {
        ops
            .applications()
            .list()
            .filter(as -> as.getRunningInstances() > 0)
            .flatMap(as -> client.applicationsV2().summary(SummaryApplicationRequest.builder().applicationId(as.getId()).build()))
            .filter(as -> isValidBuildpack(as.getBuildpack(), as.getDetectedBuildpack()))
            .doOnNext(as -> log.info("restaging " + as.getName() + '.'))
            .flatMap(as -> ops.applications().restage(RestageApplicationRequest.builder().name(as.getName()).build()))
            .subscribe();
    }
}
```

The application is relatively straightforward. It uses the `CloudFoundryOperations` to:

* load all the applications
* filter the ones that are running right now 
* load a `SummaryApplicationRequest` for the running application 
* determine if the application uses the buildpack we're looking for (the Python buildpack, in this case)
* determine their buildpack
* and then restage them. 

Pretty cool, eh? The programmable platform at work! Now, there's _zero_ overhead associated with programming the platform.
# Spring Cloud Blue Green Sample

## About

This repo demonstrates how to do a Blue/Green deployment when using Spring
Cloud.  It contains two apps

1.  Blue Green Service - This is a service that returns a color based on which
profile is active.  When the `blue` profile is active it will return the color
blue.  When the `green` profile is active it will return the color green.

```
$ curl "http://localhost:8181/"
{"id":"blue"}
$ curl "http://localhost:8080/"
{"id":"green"}
```

2.  Blue Green Frontend - This is a service which calls the Blue Green Service
(via Ribbon) and displays the color in a simple web app.

## Running The Sample Locally

You will need an Eureka server running on `localhost:8761`.  The easiest way to
get an Eureka server running is to use the
[Spring Cloud CLI](https://cloud.spring.io/spring-cloud-cli/) and just run
`spring cloud eureka`.

Then run the the Blue Green Service app.  You are going to want to run
two instances of the app.

Create an executable jar by running
```
cd blueorgreenservice && ./mvnw clean package && cd ../
```

First lets run an instance of the Blue Green Service with the `blue` profile activated.
```
java -jar blueorgreenservice/target/blueorgreen-0.0.1-SNAPSHOT.jar --spring.profiles.active=blue
```

Once the app is up and running you should see the service registered with Eureka,
however the status of the service will be `OUT_OF_SERVICE`.  This is to
demonstrate that when you initially bring up a service you probably want
it to be `OUT_OF_SERVICE` until you determine that it is working properly and want
traffic routed to the service.  For the sake of this demonstration, lets assume
the service is working properly and set the status to `UP` so traffic can be
routed to it.  To do this we can use the `/service-registry` endpoint.

```
curl -X "POST" "http://localhost:8181/service-registry/instance-status" \
   -H "Content-Type: text/plain; charset=utf-8" \
   -d "UP"
```

Once you make the `POST` request you can check Eureka again and the status should
now be `UP`.

Now lets bring the frontend app up.  First build an executable jar.
```
cd blueorgreenfrontend && ./mvnw clean package && cd ../
```

Now run the app.
```
java -jar blueorgreenfrontend/target/blueorgreenfrontend-0.0.1-SNAPSHOT.jar
```

Once the app is running you should be able to go to `http://localhost:9090`
and see a page that says `BLUE` with a blueish background.

Now lets say we make a change to the Blue Green Service and we want to test it
out while keeping the old version running.  
Really we won't make a change but just run the Blue Green Service with the
`green` profile.

```
java -jar blueorgreenservice/target/blueorgreen-0.0.1-SNAPSHOT.jar --spring.profiles.active=green
```

Once the app is up and running you can go to Eureka and see there are now two
instances of the `bluegreen` service running but one has the status `UP` and the
other with the status of `OUT_OF_SERVICE`.  Since the `green` version of the Blue
Green Service currently has the status `OUT_OF_SERVICE` the frontend web app won't make any
requests to that instance.  Again this gives us the opportunity to make sure the `green`
version of the Blue Green Service is running correctly before we change its status
to `UP` and allowing traffic to be sent to it.  Lets assume its running fine and
change its status.

```
curl -X "POST" "http://localhost:8080/service-registry/instance-status" \
   -H "Content-Type: text/plain; charset=utf-8" \
   -d "UP"
```

Now in Eureka we should see that both instances of the Blue Green Service have the
status `UP`.  However the frontend web app won't route any traffic to the service until
it refreshes the service data from Eureka.  This may take a couple minutes to occur.
Once the frontend app refreshes it's service data from Eureka, you can refresh the
web app and see that sometimes you will see `BLUE` returned and sometimes you will
see `GREEN` returned.  They should alternate back and forth due to the round robin
load balancing algorithm that is used in Ribbon by default.

Once you are satisfied the the `green` version of the Blue Green Service is running
you will want to bring down the `blue` version.  This can be done in two ways.

1.  You can change the status of the `blue` service to `OUT_OF_SERVICE` and wait
for the status to propagate to the frontend web app so that only traffic will only be
routed to the `green` service.  Once that happens you can shut down the `blue` service
without worrying about any traffic being routed to the service.

2.  You can just shut down the `blue` service without first changing the status.
If you choose this option the frontend web app might still route traffic to the
`blue` service even though it is not running anymore because it has not gotten
updated service information from Eureka.  This problem can be mitigated by enabling
[Spring Retry](http://cloud.spring.io/spring-cloud-static/Dalston.SR2/#retrying-failed-requests)
in the frontend web app so it retries any failed requests to the
Blue Green Service.

Lets take the more conservative approach and change the status of the `blue`
service to `OUT_OF_SERVICE` before shutting it down.

```
curl -X "POST" "http://localhost:8181/service-registry/instance-status" \
   -H "Content-Type: text/plain; charset=utf-8" \
   -d "OUT_OF_SERVICE"
```

Again the frontend web app will continue to route traffic to the `blue` version
of the service until it gets updated service information from Eureka.  Once
this occurs you will only see `GREEN` displayed in the webapp.  At this point
you can safely shut down the `blue` version of the service.

## Running The Sample On Cloud Foundry

Cloud Foundry offers a more real world blue/green deployment scenario more easily
than trying the same locally.  The functionality it offers as far as scaling apps
and managing routes allows for more complicated deployment scenarios.

In this sample I am going to leverage Spring Cloud Services to deploy a
service registry service that the apps will use.  This is essentially a productized
version of Eureka running on Pivotal Cloud Foundry.  You are welcome to use
plain old Eureka if you want to deploy it to Cloud Foundry.  If you would like
to follow along with the sample below you can signup for a free account
on our cloud hosted version of [Pivotal Web Services](http://run.pivotal.io)
and do the exact same steps.

First you will need to create a [Service Registry service](http://docs.pivotal.io/spring-cloud-services/1-4/common/service-registry/)
from Spring Cloud Services.  See the docs for information on how to do that.  The
important part when creating the service is to give it the name `bluegreen-registry`
or else when you deploy the apps the deployment will fail.

To leverage this service you will need to add the [Spring Cloud Service starter
to your POM](http://docs.pivotal.io/spring-cloud-services/1-4/common/service-registry/writing-client-applications.html).  
The projects in this repo already have this so no need to do that.

You will also need the [Cloud Foundry CLI](http://docs.run.pivotal.io/cf-cli/)
installed to deploy the apps.  Follow these [instructions](http://docs.run.pivotal.io/cf-cli/install-go-cli.html)
to install the CLI.

Once you have the CLI installed we need to login.

```
$ cf login -a https://api.run.pivotal.io
```

NOTE: Substitute your Cloud Foundry API URL if you are not using Pivotal Web Services.

You will be prompted for your email address and password to login.  After you
login you MAY also be prompted to select and organization and space.  This will
depend on your account.

Now that you are logged in you can deploy the applications. Run the following
commands

```
$ cd blueorgreenservice && ./mvnw clean package && cf push && ../
```

This will use the `manifest.yml` file to deploy the
applications to Cloud Foundry.  You may run into issues during the deployment if
there are other apps deployed to your Cloud Foundry instance already using the
host names `blueservice` and/or `greenservice`.  If this
is the case, open the `manifest.yml` file and change the values in the `hosts`
field so they are unique in your Cloud Foundry deployment.

The apps should automatically register with the SCS service discovery registry.
If you check the management console for the service discovery registry you will
notice that both apps are registered with an `OUT_OF_SERVICE` status.  Lets
assume the `blue` service is running fine and change the status to `UP`.

```
curl -X "POST" "https://blueservice.mycf.com/service-registry/instance-status" \
     -H "Content-Type: text/plain; charset=utf-8" \
     -d "UP"
```

Make sure that you change the URL in the above cURL command to be the correct one
for your `blue` service.

We will leave the `green` service with the status `OUT_OF_SERVICE` for now.

Now lets deploy the frontend web app.
```
$ cd blueorgreenfrontend && ./mvnw clean package && cf push && ../
```

Again you may run into issues if the host name is already in use.
If that is he case open the `manifest.yml` file and modify the `hosts` property.

Once the frontend web app is deployed you should be able to open the app using the
URL from Cloud Foundry and see the web app in your browser.  Right now, since the
`green` service is still labeled as `OUT_OF_SERVICE` you will just see `BLUE`
displayed in the browser.  

Lets change the status of the `green` service to `UP`.

```
curl -X "POST" "https://greenservice.mycf.com/service-registry/instance-status" \
     -H "Content-Type: text/plain; charset=utf-8" \
     -d "UP"
```

Make sure that you change the URL in the above cURL command to be the correct one
for your `green` service.

You will have to wait for the frontend web app to refresh it's data from the service
registry before it knows the `green` service is now available but once that happens
you should be able to refresh the web app and see that the result will alternate
between `BLUE` and `GREEN`.

Now we can disable the `blue` service.  We have several options to do that.

1. Change the status of the `blue` service to `OUT_OF_SERVICE` and wait for that
to propagate to the frontend web app.  After that happens we can stop/delete the app.

2. We can stop or delete the `blue` service.  If you choose this option the frontend web app might still route traffic to the
`blue` service even though it is not running anymore because it has not gotten
updated service information from Eureka.  This problem can be mitigated by enabling
[Spring Retry](http://cloud.spring.io/spring-cloud-static/Dalston.SR2/#retrying-failed-requests)
in the frontend web app so it retries any failed requests to the
Blue Green Service.

Lets take the more conservative approach and change the status of the `blue` service
to `OUT_OF_SERVICE`.

```
curl -X "POST" "https://blueservice.mycf.com/service-registry/instance-status" \
     -H "Content-Type: text/plain; charset=utf-8" \
     -d "OUT_OF_SERVICE"
```     

This status change may take a few minutes to propagate to the front end web app.
Once this happens the web app will only display `GREEN`.  At this point
it is safe to shut down or delete the `blue` service.

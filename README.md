# Spring Cloud Blue Green Sample

## About

The demonstrates how to possibly do a Blue/Green deployment when using Spring
Cloud.  It contains two apps

1.  Blue Green Service - This is a service that returns a color based on which
profile is active.  When the `blue` profile is active it will return the color
blue.  When the `green` profile is active it will return the color green.

2.  Blue Green Frontend - This is a service which calls the Blue Green Service
(via Ribbon) and displays the color in a simple web app.

## Running The Sample

You will need an Eureka server running on `localhost:8761`.  The easiest way to
get an Eureka server running is to use the
[Spring Cloud CLI](https://cloud.spring.io/spring-cloud-cli/) and just run
`spring cloud eureka`.

Then run the the Blue Green Service app.  You are going to want to run
two instances of the app, one run with the `green` profile and one run with the
`blue` profile.

Create an executable jar we can run by running `./bluegreenservice/mvnw package`.

First lets run the service with the `blue` profile.
`java -jar bluegreenservice/target/bluegreen.jar -Dspring.profiles.active=blue`

Once the app is up and running you should see the service registered with Eureka,
however the status of the service will be `OUT_OF_SERVICE`.  This is to
demonstrate that when you initially bring up a service you probably want
it to be `OUT_OF_SERVICE` until you determine that it is working properly and want
traffic routed to the service.  Lets assume the service is working properly
and set the status to `UP` so traffic can be routed to it.  To do this we can
use the `/service-registry` endpoint.

```
curl -X "POST" "http://localhost:8181/service-registry/instance-status" \
   -H "Content-Type: text/plain; charset=utf-8" \
   -d "UP"
```

After that `POST` request you can check Eureka again and the status should
now be `UP`.

Now lets bring the frontend app up.  First build an executable jar.
`./bluegreenfrontend/mvnw package`

Now run the app.
`java -jar bluegreenfrontend/target/bluegreenfrontend.jar`

Once the app is running you should be able to go to `http://localhost:9090`
and see a page that says `BLUE` with a blueish background.

Now lets say we make a change to the Blue Green Service and we want to test it
out.  Really we won't make a change but just run the Blue Green Service with the
`green` profile.

`java -jar bluegreenservice/target/bluegreen.jar -Dspring.profiles.active=green`

Once the app is up and running you can go to Eureka and see there are now two
instances of the bluegreen service running but one has the status `UP` and the
other with the status of `OUT_OF_SERVICE`.  Since the green version of the Blue
Green Service currently has the status `OUT_OF_SERVICE` the webapp won't make any
requests to the service.  Again this gives us the opportunity to make sure the green
version fo the Blue Green Service is running correctly before we change its status
to `UP` and allowing traffic to be sent to it.  Lets assume its running fine and
change its status.

```
curl -X "POST" "http://localhost:8080/service-registry/instance-status" \
   -H "Content-Type: text/plain; charset=utf-8" \
   -d "UP"
```

Now in Eureka we should see that both instances of the Blue Green Service have the
status `UP`.  However the frontend app won't wrote any traffic to the service until
it refreshes the service data from Eureka.  This may take a couple minutes to occur.
Once the frontend app refreshes it's service data from Eureka, you can refresh the
web app and see that sometimes you will see `BLUE` returned and sometimes you will
see `GREEN` returned.  They should alternate back and forth due to the round robbin
load balancing algorithm that is used in Ribbon by default.

Once you are satisfied the the `green` version of the Blue Green Service is running
you will want to bring down the `blue` version.  This can be done in two ways.

1.  You can change the status of the `blue` service to `OUT_OF_SERVICE` and wait
for the status to propogate to the frontend web app so that only traffic will be
routed to the `green` service.  Once that happens you can shut down the `blue` service
without worrying about any traffic being routed to the service.

2.  You can just shut down the `blue` service without first changing the status.
If you choose this route the frontend web app might still route traffic to the
`blue` service even though it is not running anymore because it has not gotten
updated service information from Eureka.  This problem can be mitigated by enabling
Spring Retry in the frontend webapp so it retries any failed requests to the
Blue Green Service.

Lets take the more conservitive approach and change the status of the `blue`
service to `OUT_OF_SERVICE` before shutting it down.

```
curl -X "POST" "http://localhost:8181/service-registry/instance-status" \
   -H "Content-Type: text/plain; charset=utf-8" \
   -d "OUT_OF_SERVICE"
```

Again the frontend webapp will continue to route traffic to the `blue` version
of the service until it gets updated service information from Eureka.  Once
this occurs you will only see `GREEN` displayed in the webapp.  At this point
you can safely shut down the `blue` version of the service.

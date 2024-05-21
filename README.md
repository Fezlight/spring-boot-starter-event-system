# Spring Boot Library implementation of Event System

[![codecov](https://codecov.io/github/Fezlight/spring-boot-starter-event-system/graph/badge.svg?token=dF6tkf1ypO)](https://codecov.io/github/Fezlight/spring-boot-starter-event-system)

## Main Purpose

This library is dedicated for Spring Boot application. Once it is included and enabled via `events.enabled` property it
will create all resources to handle event listening using RabbitMQ in your application. To avoid losing the message we
use `spring-modulith-events` and the database backed system.

## Requirement

- Java 17+
- Spring Boot 3+
- RabbitMQ

## Getting Started

The library is published on Maven Central. Current version is `0.1.0`

Maven

```xml

<dependency>
    <groupId>fr.fezlight</groupId>
    <artifactId>spring-boot-starter-event-system</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle

```groovy
    implementation 'fr.fezlight:spring-boot-starter-event-system:0.1.0'
```

See on [Sonatype Maven Central](https://search.maven.org/artifact/fr.fezlight/spring-boot-starter-event-system) for
versions.

The library provides autoconfigured support for creating a basic implementation of an event handling system based on
property `events.enabled`, by default true.

By design the event system need a database to work, you have 2 starters available for that.

### Jdbc

Maven

```xml

<dependency>
    <groupId>fr.fezlight</groupId>
    <artifactId>spring-boot-starter-event-system-jdbc</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle

```groovy
    implementation 'fr.fezlight:spring-boot-starter-event-system-jdbc:0.1.0'
```

### MongoDB

Maven

```xml

<dependency>
    <groupId>fr.fezlight</groupId>
    <artifactId>spring-boot-starter-event-system-mongodb</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle

```groovy
    implementation 'fr.fezlight:spring-boot-starter-event-system-mongodb:0.1.0'
```

## Usage

Once you have `events.enabled=true`, you can use the `@SubscribeEvent` annotation to listen automatically when an event
is fired within your application.

Basic Example

```java
import org.springframework.stereotype.Component;

@Component
public class SampleEventListener {

    @SubscribeEvent
    public void handleOrderValidated(OrderValidatedEvent event) {
        // Do some work with event ...
    }

}
```

In this example, the annotation `@SubscribeEvent` will implicitly register this method as the handler for any *
*OrderValidatedEvent**.

> The class must be registered as a valid spring beans to be eligible for subscribing events.

### Fire an event

To fire an event, first you have to define a class corresponding to your event (e.g OrderValidatedEvent). This class
must implement **Event** interface.

```java
public class OrderValidatedEvent implements Event {
    private String id;
    // Some others properties
}
```

Once you have defined the event class, you will be able to fire event using the `ApplicationEventPublisher` bean.

```java
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class OrderService {
    private ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactionnal
    public void validateOrder(/* Some properties */) {
        // Do validate work ...

        var event = new OrderValidatedEvent();
        eventPublisher.publishEvent(event);
    }

}
```

By default, `spring-modulith` register a `ApplicationEventPublisher` bean with a TransactionalEventPublisher
implementation and place event publishing after transaction complete to guarantee good consistency between your
application and event system.

### Retry an event

By default, an annotated method will not being able to retry an event listening **automatically**.

If you want to change this behavior you can use the `retry` parameter within `@SubscribeEvent`.

```java
import org.springframework.stereotype.Component;

@Component
public class SampleEventListener {

    @SubscribeEvent(retry = 5)
    public void handleOrderValidated(OrderValidatedEvent event) {
        // Do some work with event ...
    }

}
```

The time between every retry is configured by `events.rabbit.queue.retry.time-between-retries` property.

## How it works ?

// TODO

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

Spring Boot Library Event System is licensed under Apache License.

[Available here](https://www.apache.org/licenses/LICENSE-2.0.txt)

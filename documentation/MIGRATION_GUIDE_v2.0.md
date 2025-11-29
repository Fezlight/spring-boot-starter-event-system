# Migration guide from 1.x to 2.x

## Requirements

- Spring version >= 7.0
- Spring Boot version >= 4.0
- Spring Modulith >= 2.0

## Jackson 3.0 (optionnal)

Following spring boot migration from Jackson v2.0 to v3.0. Now you have to initialize a **JsonMapper** bean instead of
the old **ObjectMapper** way for JSON-specific object.

**ObjectMapper** bean remains compatible with this version.

Before :

```java

@Bean
public ObjectMapper objectMapper() {
    var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return objectMapper;
}
```

After :

```java

@Bean
public JsonMapper jsonMapper() {
    return JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .findAndAddModules()
            .build();
}
```

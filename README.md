The project showcases [this pull](https://github.com/zalando/logbook/pull/1589) request in Logbook.

It uses two JWT tokens in request as per
[issue 381](https://github.com/zalando/logbook/issues/381):

1. A token with a proper `sub` claim:

```json
{
  "sub": "stups_sales-order-service",
  "https://identity.zalando.com/realm": "services",
  "https://identity.zalando.com/token": "Bearer",
  "azp": "stups_sales-order-service_389e4e16-0695-45df-9afd-d9be0ffab456",
  "https://identity.zalando.com/bp": "810d1d00-4312-43e5-bd31-d8373fdd24c7",
  "iss": "https://identity.zalando.com",
  "exp": 1541411315,
  "iat": 1541407705,
  "https://identity.zalando.com/privileges": [
    "com.zalando::loyalty_point_account.read_all"
  ]
}
```

2. A token with UUID in the `sub` claim, but an alternative claim (here, `https://identity.zalando.com/managed-id`)
   where the subject is a human-readable account:

```json
{
  "sub": "3b66d47c-d886-4c63-a0b9-9ec3cad7e848",
  "https://identity.zalando.com/realm": "users",
  "https://identity.zalando.com/token": "Bearer",
  "https://identity.zalando.com/managed-id": "wschoenborn",
  "azp": "ztoken",
  "https://identity.zalando.com/bp": "810d1d00-4312-43e5-bd31-d8373fdd24c7",
  "auth_time": 1540188140,
  "iss": "https://identity.zalando.com",
  "exp": 1541411248,
  "iat": 1541407638
}
```

The `Main` class uses a `CompositeAttributeExtractor` to delegate everything to the `WithoutBodyStrategy` class:

```java
final AttributeExtractor jwtFirstMatchingClaimExtractor = JwtFirstMatchingClaimExtractor.builder()
        .claimNames(Arrays.asList("https://identity.zalando.com/managed-id", "sub"))
        .build();

final AttributeExtractor jwtAllMatchingClaimsExtractor = JwtAllMatchingClaimsExtractor.builder()
        .claimNames(Arrays.asList("iss", "exp", "iat"))
        .build();

final List<AttributeExtractor> list = List.of(
        jwtFirstMatchingClaimExtractor,
        jwtAllMatchingClaimsExtractor,
        new RespAttributeExtractor()
        );

final Logbook logbook = Logbook.builder()
        .strategy(new WithoutBodyStrategy())
        .attributeExtractor(new CompositeAttributeExtractor(list))
        .sink(new DefaultSink(
        new JsonHttpLogFormatter(),
        new DefaultHttpLogWriter()
        ))
        .build();
```

The above extractor will first look for a `managed-id` claim, and then for a `sub` claim.

Using a `JsonHttpLogFormatter`, the following output is generated (two request-response pairs, each with a different
token):

```
04:11:41.300 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"local","type":"request","correlation":"91f87ec9c11e257b","protocol":"HTTP/1.1","remote":"localhost","method":"GET","uri":"https://example.com/","host":"example.com","path":"/","scheme":"https","port":null,"attributes":{"subject":"stups_sales-order-service"},"headers":{"Authorization":["XXX"]}}
04:11:41.863 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"remote","type":"response","correlation":"91f87ec9c11e257b","duration":679,"protocol":"HTTP/1.1","status":200,"attributes":{"phrase":"OK"},"headers":{"Accept-Ranges":["bytes"],"Age":["438563"],"Cache-Control":["max-age=604800"],"Content-Type":["text/html; charset=UTF-8"],"Date":["Sun, 06 Aug 2023 02:11:40 GMT"],"Etag":["\"3147526947+gzip\""],"Expires":["Sun, 13 Aug 2023 02:11:40 GMT"],"Last-Modified":["Thu, 17 Oct 2019 07:18:26 GMT"],"Server":["ECS (dcb/7EA2)"],"Vary":["Accept-Encoding"],"X-Cache":["HIT"]}}
-------------------------------
04:11:41.864 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"local","type":"request","correlation":"e1a41dca359fca53","protocol":"HTTP/1.1","remote":"localhost","method":"GET","uri":"https://example.com/","host":"example.com","path":"/","scheme":"https","port":null,"attributes":{"subject":"wschoenborn"},"headers":{"Authorization":["XXX"]}}
04:11:42.243 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"remote","type":"response","correlation":"e1a41dca359fca53","duration":379,"protocol":"HTTP/1.1","status":200,"attributes":{"phrase":"OK"},"headers":{"Accept-Ranges":["bytes"],"Age":["438563"],"Cache-Control":["max-age=604800"],"Content-Type":["text/html; charset=UTF-8"],"Date":["Sun, 06 Aug 2023 02:11:40 GMT"],"Etag":["\"3147526947+gzip\""],"Expires":["Sun, 13 Aug 2023 02:11:40 GMT"],"Last-Modified":["Thu, 17 Oct 2019 07:18:26 GMT"],"Server":["ECS (dcb/7EA2)"],"Vary":["Accept-Encoding"],"X-Cache":["HIT"]}}
```

Notice the following in the request section:
- `"attributes":{"subject":"stups_sales-order-service"}`
- `"attributes":{"subject":"wschoenborn"}`

And the following in the response section:
- `attributes":{"phrase":"OK"}`

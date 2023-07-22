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

The `Main` class uses a `ForwardingStrategy` to delegate everything to the `WithoutBodyStrategy` class, ]
while applying the following `RequestAttributesExtractor` to extract subject claims:

```java
JwtClaimExtractor(Arrays.asList("https://identity.zalando.com/managed-id","sub"))
```

The above extractor will first look for a `managed-id` claim, and then for a `sub` claim.

Using a `JsonHttpLogFormatter`, the following output is generated (two request-response pairs, each with a different
token):

```
01:24:34.087 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"local","type":"request","correlation":"da7a3ee1a275d71c","protocol":"HTTP/1.1","remote":"localhost","method":"GET","uri":"https://example.com/","host":"example.com","path":"/","scheme":"https","port":null,"attributes":{"subject":"stups_sales-order-service"},"headers":{"Authorization":["XXX"]}}
01:24:34.654 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"remote","type":"response","correlation":"da7a3ee1a275d71c","duration":662,"protocol":"HTTP/1.1","status":200,"headers":{"Accept-Ranges":["bytes"],"Age":["291354"],"Cache-Control":["max-age=604800"],"Content-Type":["text/html; charset=UTF-8"],"Date":["Sat, 22 Jul 2023 23:24:25 GMT"],"Etag":["\"3147526947+gzip\""],"Expires":["Sat, 29 Jul 2023 23:24:25 GMT"],"Last-Modified":["Thu, 17 Oct 2019 07:18:26 GMT"],"Server":["ECS (dcb/7EA2)"],"Vary":["Accept-Encoding"],"X-Cache":["HIT"]}}
-------------------------------
01:24:34.655 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"local","type":"request","correlation":"dd396fc2e4a612eb","protocol":"HTTP/1.1","remote":"localhost","method":"GET","uri":"https://example.com/","host":"example.com","path":"/","scheme":"https","port":null,"attributes":{"subject":"wschoenborn"},"headers":{"Authorization":["XXX"]}}
01:24:35.012 [main] TRACE org.zalando.logbook.Logbook -- {"origin":"remote","type":"response","correlation":"dd396fc2e4a612eb","duration":355,"protocol":"HTTP/1.1","status":200,"headers":{"Accept-Ranges":["bytes"],"Age":["291355"],"Cache-Control":["max-age=604800"],"Content-Type":["text/html; charset=UTF-8"],"Date":["Sat, 22 Jul 2023 23:24:26 GMT"],"Etag":["\"3147526947+gzip\""],"Expires":["Sat, 29 Jul 2023 23:24:26 GMT"],"Last-Modified":["Thu, 17 Oct 2019 07:18:26 GMT"],"Server":["ECS (dcb/7EA2)"],"Vary":["Accept-Encoding"],"X-Cache":["HIT"]}}
```

Notice the following in the request section:
- `"attributes":{"subject":"stups_sales-order-service"}`
- `"attributes":{"subject":"wschoenborn"}`

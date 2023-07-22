package org.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.LoggerFactory;
import org.zalando.logbook.ForwardingStrategy;
import org.zalando.logbook.attributes.JwtClaimExtractor;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.attributes.RequestAttributesExtractor;
import org.zalando.logbook.Strategy;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.core.WithoutBodyStrategy;
import org.zalando.logbook.httpclient5.LogbookHttpExecHandler;
import org.zalando.logbook.json.JsonHttpLogFormatter;

import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings({"resource", "deprecation"})
@Slf4j
public class Main {
    static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger1 = loggerContext.getLogger("org.zalando");
        Logger logger2 = loggerContext.getLogger("org.apache");
        logger1.setLevel(Level.TRACE);
        logger2.setLevel(Level.INFO);
    }

    private static final String serviceToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjA2Nzk2MDAwMzJlOTI5YzE5ZTQ4Njk1ZDBhMzk2ZWJhIn0.eyJzdWIiOiJzdHVwc19zYWxlcy1vcmRlci1zZXJ2aWNlIiwiaHR0cHM6Ly9pZGVudGl0eS56YWxhbmRvLmNvbS9yZWFsbSI6InNlcnZpY2VzIiwiaHR0cHM6Ly9pZGVudGl0eS56YWxhbmRvLmNvbS90b2tlbiI6IkJlYXJlciIsImF6cCI6InN0dXBzX3NhbGVzLW9yZGVyLXNlcnZpY2VfMzg5ZTRlMTYtMDY5NS00NWRmLTlhZmQtZDliZTBmZmFiNDU2IiwiaHR0cHM6Ly9pZGVudGl0eS56YWxhbmRvLmNvbS9icCI6IjgxMGQxZDAwLTQzMTItNDNlNS1iZDMxLWQ4MzczZmRkMjRjNyIsImlzcyI6Imh0dHBzOi8vaWRlbnRpdHkuemFsYW5kby5jb20iLCJleHAiOjE1NDE0MTEzMTUsImlhdCI6MTU0MTQwNzcwNSwiaHR0cHM6Ly9pZGVudGl0eS56YWxhbmRvLmNvbS9wcml2aWxlZ2VzIjpbImNvbS56YWxhbmRvOjpsb3lhbHR5X3BvaW50X2FjY291bnQucmVhZF9hbGwiXX0.jTXULTWNVqgOgcLA5sJ3Xp4Qdicm8JZBRadReD8CbgRXyPkaUYmY9D2W9fuzRWHA_O1K5AxlTWMQOfa2vZykfg";
    private static final String userToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IjA2Nzk2MDAwMzJlOTI5YzE5ZTQ4Njk1ZDBhMzk2ZWJhIn0.eyJzdWIiOiIzYjY2ZDQ3Yy1kODg2LTRjNjMtYTBiOS05ZWMzY2FkN2U4NDgiLCJodHRwczovL2lkZW50aXR5LnphbGFuZG8uY29tL3JlYWxtIjoidXNlcnMiLCJodHRwczovL2lkZW50aXR5LnphbGFuZG8uY29tL3Rva2VuIjoiQmVhcmVyIiwiaHR0cHM6Ly9pZGVudGl0eS56YWxhbmRvLmNvbS9tYW5hZ2VkLWlkIjoid3NjaG9lbmJvcm4iLCJhenAiOiJ6dG9rZW4iLCJodHRwczovL2lkZW50aXR5LnphbGFuZG8uY29tL2JwIjoiODEwZDFkMDAtNDMxMi00M2U1LWJkMzEtZDgzNzNmZGQyNGM3IiwiYXV0aF90aW1lIjoxNTQwMTg4MTQwLCJpc3MiOiJodHRwczovL2lkZW50aXR5LnphbGFuZG8uY29tIiwiZXhwIjoxNTQxNDExMjQ4LCJpYXQiOjE1NDE0MDc2Mzh9.vSOhtVXyNggE7IpDcEDpjM-C4h0nz5FdISsYa6iE3I6cjVLEU0oHGM5sIOaKz7UgOkUStp-aVOxWgzOeoKQunA";


    public static void main(String[] args) throws IOException {
        Logbook logbook = Logbook.builder()
                .strategy(new MyStrategy())
                .sink(new DefaultSink(
                        new JsonHttpLogFormatter(),
                        new DefaultHttpLogWriter()
                ))
                .build();

        CloseableHttpClient httpclient = HttpClientBuilder.create()
                .addExecInterceptorFirst("Logbook", new LogbookHttpExecHandler(logbook))
                .build();

        HttpGet httpGet1 = new HttpGet("https://example.com/");
        httpGet1.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken);
        httpclient.execute(httpGet1);

        System.out.println("-------------------------------");

        HttpGet httpGet2 = new HttpGet("https://example.com/");
        httpGet2.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + userToken);
        httpclient.execute(httpGet2);
    }
}

class MyStrategy implements ForwardingStrategy {

    @Override
    public Strategy delegate() {
        return new WithoutBodyStrategy();
    }

    @Override
    public RequestAttributesExtractor getRequestAttributesExtractor() {
        return new JwtClaimExtractor(Arrays.asList("https://identity.zalando.com/managed-id", "sub"));
    }
}

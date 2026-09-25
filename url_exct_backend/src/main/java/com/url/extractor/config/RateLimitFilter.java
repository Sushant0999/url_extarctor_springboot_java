package com.url.extractor.config;

import com.url.extractor.utils.MyLogger;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Filter("/**")
public class RateLimitFilter implements HttpServerFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String ipAddress = (remoteAddress != null && remoteAddress.getAddress() != null)
                ? remoteAddress.getAddress().getHostAddress()
                : "127.0.0.1";

        MyLogger.info("RateLimit Check: " + ipAddress + " for " + request.getUri());

        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            return chain.proceed(request);
        } else {
            MyLogger.warn("RATE LIMIT EXCEEDED: " + ipAddress);
            MutableHttpResponse<String> response = HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Too many requests. Please wait a minute.\"}");
            return Mono.just(response);
        }
    }
}

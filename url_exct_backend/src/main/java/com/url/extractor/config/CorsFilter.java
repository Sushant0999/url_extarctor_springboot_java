package com.url.extractor.config;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@Filter("/**")
public class CorsFilter implements HttpServerFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String origin = request.getHeaders().get(HttpHeaders.ORIGIN);

        if (request.getMethod() == HttpMethod.OPTIONS) {
            MutableHttpResponse<?> res = HttpResponse.ok();
            applyCorsHeaders(res, origin);
            return Mono.just(res);
        }

        return Mono.from(chain.proceed(request)).map(res -> {
            applyCorsHeaders(res, origin);
            return res;
        });
    }

    private void applyCorsHeaders(MutableHttpResponse<?> response, String origin) {
        String allowOrigin = (origin != null && !origin.isBlank()) ? origin : "*";
        response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
        response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }
}

package com.ai.e2e.tx.example.gateway;

import com.ai.e2e.tx.example.gateway.common.router.AbstractRouter;
import com.ai.e2e.tx.example.gateway.common.router.RouteId;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class SampleRouter extends AbstractRouter {

  @Bean
  public RouteLocator routeLocator(final RouteLocatorBuilder routeLocatorBuilder) {
    final RouteLocatorBuilder.Builder routeBuilder = routeLocatorBuilder.routes();

    routeBuilder.route(RouteId.TEST_POST.toString(),
        route -> route.path("/example")
            .and()
            .method(HttpMethod.POST)
            .filters(f -> f.prefixPath(prefix()))
            .uri(getBaseUrl())
            .filters(defaultRetryFilter(), defaultHeadersForwardingFilter(), defaultHystrixForwardingFilter())
    );

    routeBuilder.route(RouteId.TEST_GET.toString(),
        route -> route.path("/example")
            .and()
            .method(HttpMethod.GET)
            .filters(f -> f.prefixPath(prefix()))
            .uri(getBaseUrl())
            .filters(defaultRetryFilter(), defaultHeadersForwardingFilter(), defaultHystrixForwardingFilter())
    );

    return routeBuilder.build();
  }

  @Override
  protected String prefix() {
    return "";
  }

  @Override
  protected String getBaseUrl() {
    return "http://localhost:9091";
  }

}

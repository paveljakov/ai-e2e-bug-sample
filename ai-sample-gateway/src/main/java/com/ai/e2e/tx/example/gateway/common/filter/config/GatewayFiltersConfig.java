package com.ai.e2e.tx.example.gateway.common.filter.config;

import com.ai.e2e.tx.example.gateway.common.config.header.ForwardedHeadersConfig;
import com.ai.e2e.tx.example.gateway.common.filter.factory.HttpHeadersForwardingOrderedGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.HystrixGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Configuration of default GatewayFilter instances, that are used when building routes.
 */
@Configuration
public class GatewayFiltersConfig {

  private final ForwardedHeadersConfig forwardedHeadersConfig;

  private final HystrixGatewayFilterFactory hystrixGatewayFilterFactory;

  public GatewayFiltersConfig(final ForwardedHeadersConfig forwardedHeadersConfig,
      final HystrixGatewayFilterFactory hystrixGatewayFilterFactory) {
    this.forwardedHeadersConfig = forwardedHeadersConfig;
    this.hystrixGatewayFilterFactory = hystrixGatewayFilterFactory;
  }

  @Bean
  public GatewayFilter defaultBasicAuthenticationGatewayFilter() {
    final AbstractNameValueGatewayFilterFactory.NameValueConfig nameValueConfig =
        new AbstractNameValueGatewayFilterFactory.NameValueConfig();
    nameValueConfig.setName(HttpHeaders.AUTHORIZATION);
    nameValueConfig.setValue("any");
    return new OrderedGatewayFilter(
        new AddRequestHeaderGatewayFilterFactory().apply(nameValueConfig),
        HttpHeadersForwardingOrderedGatewayFilterFactory.ORDER_OF_FILTER - 1
    );
  }

  @Bean
  public GatewayFilter defaultHeadersForwardingGatewayFilter() {
    return new HttpHeadersForwardingOrderedGatewayFilterFactory().apply(forwardedHeadersConfig);
  }

  @Bean
  public GatewayFilter defaultRetryGatewayFilter() {
    return new RetryGatewayFilterFactory().apply(new RetryGatewayFilterFactory.RetryConfig());
  }

  @Bean
  public GatewayFilter defaultHystrixGatewayFilter() {
    final HystrixGatewayFilterFactory.Config config = hystrixGatewayFilterFactory.newConfig();
    config.setName("default");
    return hystrixGatewayFilterFactory.apply(config);
  }

}

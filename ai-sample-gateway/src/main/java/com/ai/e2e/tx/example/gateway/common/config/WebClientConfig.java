package com.ai.e2e.tx.example.gateway.common.config;

import com.ai.e2e.tx.example.gateway.common.filter.function.HttpHeadersForwardingFilterFunction;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.TimeUnit;

/**
 * Configuration of WebClient instance that should be used for implementing custom gateway endpoints.
 */
@Configuration
public class WebClientConfig {

  private final Integer readTimeoutInMilliSeconds;
  private final Integer writeTimeoutInMilliSeconds;
  private final Integer connectTimeoutInMilliSeconds;

  private final ExchangeFilterFunction httpHeadersForwardingFilterFunction;

  public static final String SAMPLE = "sample";

  public WebClientConfig(@Value("${http.read.timeoutInMilliSeconds:5000}") final Integer readTimeoutInMilliSeconds,
      @Value("${http.write.timeoutInMilliSeconds:5000}") final Integer writeTimeoutInMilliSeconds,
      @Value("${http.connect.timeoutInMilliSeconds:5000}") final Integer connectTimeoutInMilliSeconds,
      final HttpHeadersForwardingFilterFunction httpHeadersForwardingFilterFunction) {
    this.readTimeoutInMilliSeconds = readTimeoutInMilliSeconds;
    this.writeTimeoutInMilliSeconds = writeTimeoutInMilliSeconds;
    this.connectTimeoutInMilliSeconds = connectTimeoutInMilliSeconds;
    this.httpHeadersForwardingFilterFunction = httpHeadersForwardingFilterFunction;
  }

  @Bean
  public HttpClient httpClient() {
    // used not only by WebClient but also by default gateway routes
    final TcpClient tcpClient = buildTcpClient();
    final HttpClient httpClient = HttpClient.from(tcpClient);
    return httpClient.compress(true);
  }

  @Bean(name = SAMPLE)
  public WebClient authorizationWebClient(final HttpClient httpClient) {
    return buildWebClient(httpClient);
  }

  private WebClient buildWebClient(final HttpClient httpClient) {
    final ClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(httpClient);
    final WebClient.Builder builder = WebClient.builder()
        .baseUrl("http://localhost:8081")
        .clientConnector(clientHttpConnector)
        .filter(httpHeadersForwardingFilterFunction);

    return builder.build();
  }

  private TcpClient buildTcpClient() {
    return TcpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutInMilliSeconds)
        .doOnConnected(
            // these might be overridden through gateway routes in TcpClientPerRouteNettyRoutingFilter
            connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(readTimeoutInMilliSeconds, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(writeTimeoutInMilliSeconds, TimeUnit.MILLISECONDS))
        );
  }

}

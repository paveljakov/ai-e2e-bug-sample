package com.ai.e2e.tx.example.gateway;

import com.nortal.healthcare.store.ExternalConfigJksKeyStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;

@Configuration
public class TestBeansConfig {

  @Value("${rest.authorization.basic.username}")
  private String username;

  @Value("${rest.authorization.basic.password}")
  private String password;

  @Bean
  public String basicAuthHeaderValue() {
    return HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8);
  }

  @Bean
  @Primary
  public ExternalConfigJksKeyStore atostekKeyStore() {
    return mock(ExternalConfigJksKeyStore.class);
  }

}

package com.ai.e2e.tx.example.gateway.common.config.header;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ForwardedHeadersConfigTest {

  private static final String ACCEPT_SOMETHING = "Accept-Something";

  private static final String CONTENT_TYPE = "Content-Type";

  private static final String AUTHORIZATION = "AuthorizatioN";

  private ForwardedHeadersConfig forwardedHeadersConfig;

  @Before
  public void setUp() throws Exception {
    forwardedHeadersConfig = new ForwardedHeadersConfig(Set.of(ACCEPT_SOMETHING, CONTENT_TYPE, AUTHORIZATION));
  }

  @Test
  public void getHeadersToForwardLowerCase() {
    final Set<String> headersToForwardLowerCase = forwardedHeadersConfig.getHeadersLowerCase();

    assertEquals(3, headersToForwardLowerCase.size());
    assertTrue(headersToForwardLowerCase.contains(ACCEPT_SOMETHING.toLowerCase()));
    assertTrue(headersToForwardLowerCase.contains(CONTENT_TYPE.toLowerCase()));
    assertTrue(headersToForwardLowerCase.contains(AUTHORIZATION.toLowerCase()));
  }

}

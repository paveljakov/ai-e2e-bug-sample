package com.ai.e2e.tx.example.gateway.atostek.notification.handler;

import com.atostek.generated.xml.PushNotificationResult;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.net.SocketException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class RequestErrorHandlerTest {

  private static final String LOG_PREFIX = UUID.randomUUID().toString();

  @InjectMocks
  private RequestErrorHandler requestErrorHandler;

  @Test
  public void success() {
    final PushNotificationResult result = requestErrorHandler.handleSuccessAndError(
        LOG_PREFIX, Mono.just("anything")).block();

    assertEquals(PushNotificationResult.SUCCESS, result);
  }

  @Test
  public void channelError() {
    final PushNotificationResult result =
        requestErrorHandler.handleSuccessAndError(LOG_PREFIX,
            Mono.error(ReadTimeoutException.INSTANCE)
        ).block();

    assertEquals(PushNotificationResult.TEMPORARY_FAILURE, result);
  }

  @Test
  public void inputOutputError() {
    final PushNotificationResult result =
        requestErrorHandler.handleSuccessAndError(LOG_PREFIX,
            Mono.error(new SocketException("bla bla"))
        ).block();

    assertEquals(PushNotificationResult.TEMPORARY_FAILURE, result);
  }

  @Test
  public void anyOtherError() {
    final PushNotificationResult result =
        requestErrorHandler.handleSuccessAndError(LOG_PREFIX,
            Mono.error(new RuntimeException("any other"))
        ).block();

    assertEquals(PushNotificationResult.PERMANENT_FAILURE, result);
  }

  @Test
  public void monoDoesNotEmitError() {
    final PushNotificationResult result = requestErrorHandler.handleSuccessAndError(
        LOG_PREFIX, Mono.empty()).block();

    assertEquals(PushNotificationResult.PERMANENT_FAILURE, result);
  }

}

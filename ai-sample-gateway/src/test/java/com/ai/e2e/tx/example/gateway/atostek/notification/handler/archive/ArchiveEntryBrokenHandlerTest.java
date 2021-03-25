package com.ai.e2e.tx.example.gateway.atostek.notification.handler.archive;

import com.atostek.generated.xml.Entry;
import com.atostek.generated.xml.Notification;
import com.atostek.generated.xml.NotificationResponse;
import com.atostek.generated.xml.PushNotificationResult;
import com.nortal.healthcare.gateway.atostek.notification.builder.NotificationResponseBuilder;
import com.nortal.healthcare.gateway.atostek.notification.handler.RequestErrorHandler;
import com.nortal.healthcare.gateway.atostek.service.AtostekFormService;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveEntryBrokenHandlerTest {

  private static final String OID = "any.oid.";

  private static final String ERROR_DESCRIPTION = "error description";

  private static final String LOG_PREFIX = UUID.randomUUID().toString();

  @Mock
  private AtostekFormService atostekFormService;

  @InjectMocks
  private ArchiveEntryBrokenHandler archiveEntryBrokenHandler;

  @Mock
  private Notification notification;

  @Mock
  private Entry entry;

  @Mock
  private RequestErrorHandler requestErrorHandler;

  @Before
  public void setUp() throws Exception {
    when(requestErrorHandler.handleSuccessAndError(eq(LOG_PREFIX), any())).thenCallRealMethod();
  }

  @Test
  public void handle() {
    when(atostekFormService.invalidate(eq(OID), eq(ERROR_DESCRIPTION))).thenReturn(Mono.just(HttpStatus.OK));
    mockNotification();

    final NotificationResponse notificationResponse =
        archiveEntryBrokenHandler.handle(notification, new NotificationResponseBuilder(LOG_PREFIX)).block().build();

    assertEquals(PushNotificationResult.SUCCESS, notificationResponse.getResult());
  }

  @Test
  public void handleTemporaryError() {
    when(atostekFormService.invalidate(eq(OID), eq(ERROR_DESCRIPTION))).thenReturn(
        Mono.error(ReadTimeoutException.INSTANCE)
    );
    mockNotification();

    final NotificationResponse notificationResponse =
        archiveEntryBrokenHandler.handle(notification, new NotificationResponseBuilder(LOG_PREFIX)).block().build();

    assertEquals(PushNotificationResult.TEMPORARY_FAILURE, notificationResponse.getResult());
  }

  @Test
  public void handlePermanentError() {
    when(atostekFormService.invalidate(eq(OID), eq(ERROR_DESCRIPTION))).thenReturn(Mono.empty());
    mockNotification();

    final NotificationResponse notificationResponse =
        archiveEntryBrokenHandler.handle(notification, new NotificationResponseBuilder(LOG_PREFIX)).block().build();

    assertEquals(PushNotificationResult.PERMANENT_FAILURE, notificationResponse.getResult());
  }

  private void mockNotification() {
    when(notification.getEntry()).thenReturn(entry);
    when(entry.getOid()).thenReturn(OID);
    when(notification.getErrorDescription()).thenReturn(ERROR_DESCRIPTION);
  }

}

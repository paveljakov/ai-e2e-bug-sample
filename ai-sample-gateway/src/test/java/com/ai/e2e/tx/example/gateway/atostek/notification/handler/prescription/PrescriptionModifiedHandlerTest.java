package com.ai.e2e.tx.example.gateway.atostek.notification.handler.prescription;

import com.atostek.generated.xml.Notification;
import com.atostek.generated.xml.PrescriptionData;
import com.atostek.generated.xml.PushNotificationResult;
import com.atostek.generated.xml.SCMedication;
import com.atostek.generated.xml.SCMedicationWrapper;
import com.nortal.healthcare.gateway.atostek.dto.PrescriptionWithMedication;
import com.nortal.healthcare.gateway.atostek.notification.builder.NotificationResponseBuilder;
import com.nortal.healthcare.gateway.atostek.notification.handler.RequestErrorHandler;
import com.nortal.healthcare.gateway.atostek.service.PrescriptionService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PrescriptionModifiedHandlerTest {

  private static final String LOG_PREFIX = UUID.randomUUID().toString();

  @Mock
  private RequestErrorHandler requestErrorHandler;

  @Mock
  private PrescriptionService prescriptionService;

  @InjectMocks
  private PrescriptionModifiedHandler prescriptionModifiedHandler;

  @Mock
  private PrescriptionData prescriptionData;

  @Mock
  private SCMedication scMedication;

  @Mock
  private SCMedicationWrapper scMedicationWrapper;

  @Mock
  private Notification notification;

  @Test
  public void handle() {
    final PrescriptionWithMedication prescriptionWithMedication =
        new PrescriptionWithMedication(prescriptionData, scMedication);
    final Mono<PrescriptionWithMedication> prescriptionWithMedicationMono = Mono.just(prescriptionWithMedication);
    when(prescriptionService.update(eq(prescriptionWithMedication))).thenReturn(prescriptionWithMedicationMono);
    when(requestErrorHandler.handleSuccessAndError(eq(LOG_PREFIX), eq(prescriptionWithMedicationMono)))
        .thenReturn(Mono.just(PushNotificationResult.SUCCESS));
    when(notification.getPrescription()).thenReturn(prescriptionData);
    when(notification.getMedication()).thenReturn(scMedicationWrapper);
    when(scMedicationWrapper.getMedication()).thenReturn(scMedication);

    final NotificationResponseBuilder notificationResponseBuilder =
        prescriptionModifiedHandler.handle(notification, new NotificationResponseBuilder(LOG_PREFIX)).block();

    assertEquals(notificationResponseBuilder.build().getResult(), PushNotificationResult.SUCCESS);
  }

}

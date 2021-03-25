package com.ai.e2e.tx.example.gateway.clinical.care.service.impl;

import com.nortal.healthcare.gateway.clinical.care.dto.AnesthesiaRealizationDto;
import com.nortal.healthcare.gateway.clinical.care.dto.builder.AnesthesiaRealizationBuilder;
import com.nortal.healthcare.gateway.clinical.care.service.ClinicalCareService;
import com.nortal.healthcare.gateway.document.service.DocumentService;
import com.nortal.healthcare.gateway.schedule.dto.ResourceDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnesthesiaRealizationServiceImplTest {

  @InjectMocks
  private AnesthesiaRealizationServiceImpl anesthesiaRealizationService;

  @Mock
  private DocumentService documentService;

  @Mock
  private ClinicalCareService clinicalCareService;

  private UUID bundleUUID = UUID.randomUUID();

  private List<UUID> questionnaireUuids = List.of(UUID.randomUUID());

  @Test
  public void saveAnesthesiaRealizationForm() {
    when(documentService.saveQuestionnaireBundle(any())).thenReturn(resultOfDocument());
    when(clinicalCareService.saveAnesthesiaRealization(any())).thenReturn(createAnesthesiaRealizationDto());

    Mono<AnesthesiaRealizationBuilder> realizationBuilderMono =
        anesthesiaRealizationService.saveAnesthesia(getAnesthesiaRealizationDto());

    assertData(realizationBuilderMono.block());
  }

  @Test
  public void updateAnesthesiaRealizationForm() {
    when(documentService.updateQuestionnaireBundle(any(), any())).thenReturn(resultOfDocument());
    when(clinicalCareService.saveAnesthesiaRealization(any())).thenReturn(createAnesthesiaRealizationDto());

    Mono<AnesthesiaRealizationBuilder> realizationBuilderMono =
        anesthesiaRealizationService.updateAnesthesia(bundleUUID, getAnesthesiaRealizationDto());

    assertData(realizationBuilderMono.block());
  }

  private Mono<ResourceDto<AnesthesiaRealizationDto>> createAnesthesiaRealizationDto() {
    AnesthesiaRealizationDto anesthesiaRealizationDto = new AnesthesiaRealizationDto();
    anesthesiaRealizationDto.setSurgeryAppointment(UUID.randomUUID());
    anesthesiaRealizationDto.setBundleUuid(bundleUUID);
    anesthesiaRealizationDto.setQuestionnaireResponsesIds(questionnaireUuids);
    return Mono.just(new ResourceDto(anesthesiaRealizationDto));
  }

  private <R> R assertData(final AnesthesiaRealizationBuilder anesthesiaRealizationBuilder) {
    assertNotNull(anesthesiaRealizationBuilder);
    assertNotNull(anesthesiaRealizationBuilder.getAnesthesiaRealizationDto());
    assertEquals(anesthesiaRealizationBuilder.getBundleUuid(), bundleUUID);
    assertEquals(anesthesiaRealizationBuilder.getQuestionnaireUuids(), questionnaireUuids);
    return null;
  }

  private Mono<Map<UUID, List<UUID>>> resultOfDocument() {
    Map<UUID, List<UUID>> resultOfDocument = new HashMap();
    resultOfDocument.put(bundleUUID, questionnaireUuids);
    return Mono.just(resultOfDocument);
  }

  private AnesthesiaRealizationDto getAnesthesiaRealizationDto() {
    final AnesthesiaRealizationDto anesthesiaRealizationDto = new AnesthesiaRealizationDto();
    anesthesiaRealizationDto.setSurgeryAppointment(UUID.randomUUID());
    anesthesiaRealizationDto.setQuestionnaireResponse("testing questionnaire");
    return anesthesiaRealizationDto;
  }

}

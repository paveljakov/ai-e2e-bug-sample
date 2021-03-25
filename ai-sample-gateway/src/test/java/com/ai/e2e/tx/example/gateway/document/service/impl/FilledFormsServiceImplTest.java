package com.ai.e2e.tx.example.gateway.document.service.impl;

import com.nortal.healthcare.commons.rest.dto.ResourceDto;
import com.nortal.healthcare.gateway.atostek.dto.AtostekFormDto;
import com.nortal.healthcare.gateway.atostek.dto.AtostekPartialFormDto;
import com.nortal.healthcare.gateway.atostek.service.AtostekFormService;
import com.nortal.healthcare.gateway.document.dto.FilledFormDto;
import com.nortal.healthcare.gateway.document.dto.FormPreviewDto;
import com.nortal.healthcare.gateway.document.service.DocumentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FilledFormsServiceImplTest {

  private static final UUID ENCOUNTER = UUID.randomUUID();

  @Mock
  private DocumentService documentService;
  @Mock
  private AtostekFormService atostekFormService;

  @InjectMocks
  private FilledFormsServiceImpl filledFormsService;

  @Test
  public void getFormsByEncounter() {

    when(documentService.getFormsByEncounter(ENCOUNTER)).thenReturn(emrForms());
    when(atostekFormService.getEncounterForms(ENCOUNTER)).thenReturn(atostekForms());

    final List<ResourceDto<FormPreviewDto>> formsByEncounter =
        filledFormsService.getFormsByEncounter(ENCOUNTER).collectList().block();

    assertEquals(5, formsByEncounter.size());
    assertEquals(3, formsByEncounter.stream().filter(f -> "EMR".equals(f.getResource().getSource())).count());
    assertEquals(2, formsByEncounter.stream().filter(f -> "ERA".equals(f.getResource().getSource())).count());
  }

  private Flux<ResourceDto<FilledFormDto>> emrForms() {
    final FilledFormDto emrForm = new FilledFormDto();
    emrForm.setFormSource("EMR");
    emrForm.setTitle("title");
    emrForm.setDraft(false);
    emrForm.setDocumentUuid(UUID.randomUUID());
    final List<ResourceDto<FilledFormDto>> resources =
        List.of(new ResourceDto(emrForm), new ResourceDto(emrForm), new ResourceDto(emrForm));
    return Flux.fromIterable(resources);
  }

  private Flux<AtostekFormDto> atostekForms() {
    final AtostekFormDto atostekForm = new AtostekFormDto();
    atostekForm.setPartialForm(new AtostekPartialFormDto());
    atostekForm.getPartialForm().setType("ATOSTEK_FORM_TYPE");
    return Flux.fromIterable(List.of(atostekForm, atostekForm));
  }

}

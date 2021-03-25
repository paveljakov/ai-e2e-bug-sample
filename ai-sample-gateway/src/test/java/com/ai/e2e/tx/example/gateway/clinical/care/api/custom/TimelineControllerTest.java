package com.ai.e2e.tx.example.gateway.clinical.care.api.custom;

import com.nortal.healthcare.commons.fhir.dto.resource.Encounter;
import com.nortal.healthcare.commons.fhir.dto.resource.Procedure;
import com.nortal.healthcare.commons.fhir.dto.resource.QuestionnaireResponse;
import com.nortal.healthcare.commons.rest.dto.Page;
import com.nortal.healthcare.gateway.clinical.care.dto.DiagnosisDto;
import com.nortal.healthcare.gateway.clinical.care.dto.DiagnosisHeaderDto;
import com.nortal.healthcare.gateway.clinical.care.dto.EncounterDto;
import com.nortal.healthcare.gateway.clinical.care.dto.TimelineEventDataDto;
import com.nortal.healthcare.gateway.clinical.care.dto.TimelineEventDto;
import com.nortal.healthcare.gateway.clinical.care.service.ClinicalCareService;
import com.nortal.healthcare.gateway.document.dto.TagDto;
import com.nortal.healthcare.gateway.document.service.DocumentService;
import com.nortal.healthcare.gateway.system.dto.PractitionerDto;
import com.nortal.healthcare.gateway.system.service.SystemService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TimelineControllerTest {

  private static final UUID PATIENT_UUID = UUID.randomUUID();

  private static final UUID ENCOUNTER_UUID = UUID.randomUUID();

  private static final ZonedDateTime DATE_FROM =
      ZonedDateTime.ofLocal(LocalDateTime.MIN, ZoneId.systemDefault(), ZoneOffset.UTC);

  private static final ZonedDateTime DATE_TO =
      ZonedDateTime.ofLocal(LocalDateTime.MAX, ZoneId.systemDefault(), ZoneOffset.UTC);

  private static final Set<String> TAG_CODES = Set.of();

  private static final int PAGE = 1;

  private static final int PAGE_SIZE = 5;

  @Mock
  private SystemService systemService;

  @Mock
  private ClinicalCareService clinicalCareService;

  @Mock
  private DocumentService documentService;

  @Mock
  private Set<UUID> encounterUuids;

  @InjectMocks
  private TimelineController timelineController;

  @Test
  public void getTimelineEventPageByPatientNoEncountersFound() {
    final Page<EncounterDto> encounterDtoPage = getEmptyEncounterPage();
    when(documentService.listEncounterUuidsByTagCodes(eq(PATIENT_UUID), eq(TAG_CODES))).thenReturn(
        Mono.just(Optional.of(encounterUuids)));
    when(clinicalCareService.listEncountersByPatient(eq(PATIENT_UUID), eq(DATE_FROM), eq(DATE_TO), eq(encounterUuids),
        eq(PAGE), eq(PAGE_SIZE)
    )).thenReturn(Mono.just(encounterDtoPage));

    final Page<TimelineEventDto> timelineEventPageByPatient =
        timelineController.getTimelineEventPageByPatient(PATIENT_UUID, DATE_FROM, DATE_TO, TAG_CODES, PAGE, PAGE_SIZE)
            .block();

    assertEqualPages(encounterDtoPage, timelineEventPageByPatient);
  }

  @Test
  public void getTimelineEventDataByEncounterEncounterNotFound() {
    when(clinicalCareService.getEncounter(eq(ENCOUNTER_UUID))).thenReturn(Mono.empty());

    final TimelineEventDataDto timelineEventDataByEncounter =
        timelineController.getTimelineEventDataByEncounter(ENCOUNTER_UUID).block();

    assertNull(timelineEventDataByEncounter);
  }

  @Test
  public void getTimelineEventPageByPatient() {
    final Page<EncounterDto> encounterDtoPage = getNonEmptyEncounterPage();
    when(documentService.listEncounterUuidsByTagCodes(eq(PATIENT_UUID), eq(TAG_CODES))).thenReturn(
        Mono.just(Optional.of(encounterUuids)));
    when(clinicalCareService.listEncountersByPatient(eq(PATIENT_UUID), eq(DATE_FROM), eq(DATE_TO), eq(encounterUuids),
        eq(PAGE), eq(PAGE_SIZE)
    )).thenReturn(Mono.just(encounterDtoPage));
    final Set<UUID> performerUuids = getPerformerUuids(encounterDtoPage);
    final List<UUID> performerList = List.copyOf(performerUuids);
    when(systemService.getPractitioners(eq(performerUuids))).thenReturn(
        Flux.fromIterable(List.of(getPractitionerDto(performerList.get(0)), getPractitionerDto(performerList.get(1)))));
    final Set<UUID> encounterUuids = getEncounterUuids(encounterDtoPage);
    when(documentService.getTagsByEncounter(eq(encounterUuids))).thenReturn(
        Mono.just(getTagDtosByEncounterDto(encounterUuids)));
    when(clinicalCareService.listDiagnosisCodesByEncounterUuid(eq(encounterUuids))).thenReturn(
        Mono.just(getDiagnosisHeaders(encounterUuids)));

    final Page<TimelineEventDto> timelineEventPageByPatient =
        timelineController.getTimelineEventPageByPatient(PATIENT_UUID, DATE_FROM, DATE_TO, TAG_CODES, PAGE, PAGE_SIZE)
            .block();

    assertEqualPages(encounterDtoPage, timelineEventPageByPatient);
    assertTimelineEventList(timelineEventPageByPatient.getList());
  }

  @Test
  public void getTimelineEventDataByEncounter() {
    final QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
    final DiagnosisDto diagnosisDto = new DiagnosisDto();
    final Procedure procedure = new Procedure();
    final Mono<Encounter> encounterMono = getEncounter(ENCOUNTER_UUID);

    when(clinicalCareService.getEncounter(eq(ENCOUNTER_UUID))).thenReturn(encounterMono);
    when(documentService.getQuestionnaireResponsesByEncounterUuid(eq(ENCOUNTER_UUID))).thenReturn(
        Mono.just(Set.of(questionnaireResponse)));
    when(clinicalCareService.getDiagnosisByEncounterUuid(eq(ENCOUNTER_UUID))).thenReturn(
        Mono.just(Set.of(diagnosisDto)));
    when(clinicalCareService.getProceduresByEncounterUuid(eq(ENCOUNTER_UUID))).thenReturn(Mono.just(Set.of(procedure)));

    final TimelineEventDataDto timelineEventDataByEncounter =
        timelineController.getTimelineEventDataByEncounter(ENCOUNTER_UUID).block();

    assertEquals(encounterMono.block(), timelineEventDataByEncounter.getEncounter());
    assertEquals(questionnaireResponse, timelineEventDataByEncounter.getQuestionnaireResponses().iterator().next());
    assertEquals(diagnosisDto, timelineEventDataByEncounter.getDiagnoses().iterator().next());
    assertEquals(procedure, timelineEventDataByEncounter.getProcedures().iterator().next());
  }

  private Mono<Encounter> getEncounter(final UUID encounterUuid) {
    final Encounter encounter = new Encounter();
    encounter.setId(encounterUuid.toString());
    return Mono.just(encounter);
  }

  private void assertTimelineEventList(final List<TimelineEventDto> list) {
    list.forEach(eventDto -> {
      assertNotNull(eventDto.getEncounter());
      assertNotNull(eventDto.getPractitioner());
      assertNotNull(eventDto.getTags());
      assertFalse(eventDto.getTags().isEmpty());
      assertNotNull(eventDto.getTimelineEventHeaderDto());
    });
  }

  private void assertEqualPages(Page<EncounterDto> encounterDtoPage,
      Page<TimelineEventDto> timelineEventPageByPatient) {
    assertEquals(encounterDtoPage.getPage(), timelineEventPageByPatient.getPage());
    assertEquals(encounterDtoPage.getPageSize(), timelineEventPageByPatient.getPageSize());
    assertEquals(encounterDtoPage.getTotalPages(), timelineEventPageByPatient.getTotalPages());
    assertEquals(encounterDtoPage.getTotalCount(), timelineEventPageByPatient.getTotalCount());
    assertEquals(encounterDtoPage.getList().size(), timelineEventPageByPatient.getList().size());
  }

  private Map<UUID, DiagnosisHeaderDto> getDiagnosisHeaders(final Set<UUID> encounterUuids) {
    return encounterUuids.stream()
        .collect(Collectors.toMap(element -> element, element -> {
          final DiagnosisHeaderDto diagnosisHeaderDto = new DiagnosisHeaderDto();
          diagnosisHeaderDto.setCode(UUID.randomUUID().toString());
          diagnosisHeaderDto.setName(UUID.randomUUID().toString());
          return diagnosisHeaderDto;
        }));
  }

  private Map<UUID, Set<TagDto>> getTagDtosByEncounterDto(final Set<UUID> encounterUuids) {
    return encounterUuids.stream()
        .collect(Collectors.toMap(element -> element, element -> getTagDtoSet()));
  }

  private Set<TagDto> getTagDtoSet() {
    return Set.of(getTagDto(), getTagDto());
  }

  private TagDto getTagDto() {
    final TagDto tagDto = new TagDto();
    tagDto.setPrimary(false);
    tagDto.setAbbreviation("abbr" + UUID.randomUUID());
    return tagDto;
  }

  private PractitionerDto getPractitionerDto(final UUID uuid) {
    final PractitionerDto practitionerDto = new PractitionerDto();
    practitionerDto.setUuid(uuid);
    return practitionerDto;
  }

  private Set<UUID> getEncounterUuids(final Page<EncounterDto> encounterDtoPage) {
    return encounterDtoPage.getList().stream().map(EncounterDto::getUuid).collect(Collectors.toSet());
  }

  private Set<UUID> getPerformerUuids(final Page<EncounterDto> encounterDtoPage) {
    return encounterDtoPage.getList().stream().map(EncounterDto::getPerformer).collect(Collectors.toSet());
  }

  private Page<EncounterDto> getNonEmptyEncounterPage() {
    final EncounterDto encounterDto1 = getEncounterDto();
    final EncounterDto encounterDto2 = getEncounterDto();

    final Page<EncounterDto> encounterDtoPage = new Page<>();
    encounterDtoPage.setPage(PAGE);
    encounterDtoPage.setPageSize(PAGE_SIZE);
    encounterDtoPage.setList(List.of(encounterDto1, encounterDto2));
    return encounterDtoPage;
  }

  private EncounterDto getEncounterDto() {
    final EncounterDto encounterDto1 = new EncounterDto();
    encounterDto1.setUuid(UUID.randomUUID());
    encounterDto1.setPerformer(UUID.randomUUID());
    return encounterDto1;
  }

  private Page<EncounterDto> getEmptyEncounterPage() {
    final Page<EncounterDto> encounterDtoPage = new Page<>();
    encounterDtoPage.setPage(PAGE);
    encounterDtoPage.setPageSize(PAGE_SIZE);
    encounterDtoPage.setTotalCount(0);
    encounterDtoPage.setTotalPages(0);
    encounterDtoPage.setList(new ArrayList<>());
    return encounterDtoPage;
  }

}

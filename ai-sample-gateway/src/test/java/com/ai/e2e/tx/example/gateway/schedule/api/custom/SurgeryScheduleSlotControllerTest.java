package com.ai.e2e.tx.example.gateway.schedule.api.custom;

import com.nortal.healthcare.commons.fhir.dto.Reference;
import com.nortal.healthcare.commons.fhir.dto.resource.Encounter;
import com.nortal.healthcare.commons.fhir.dto.resource.Location;
import com.nortal.healthcare.gateway.clinical.care.dto.FacetDto;
import com.nortal.healthcare.gateway.clinical.care.dto.RiskLevel;
import com.nortal.healthcare.gateway.clinical.care.dto.surgery.appointment.AppointmentInfoDto;
import com.nortal.healthcare.gateway.clinical.care.dto.surgery.appointment.SurgeryAppointmentDto;
import com.nortal.healthcare.gateway.clinical.care.service.ClinicalCareService;
import com.nortal.healthcare.gateway.common.dto.ResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.AppointmentDto;
import com.nortal.healthcare.gateway.schedule.dto.LocationSurgerySlotsDto;
import com.nortal.healthcare.gateway.schedule.dto.SchedulableResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.SurgerySlotDto;
import com.nortal.healthcare.gateway.schedule.dto.TimeSlotDto;
import com.nortal.healthcare.gateway.schedule.dto.enums.AppointmentType;
import com.nortal.healthcare.gateway.schedule.dto.enums.ResourceType;
import com.nortal.healthcare.gateway.schedule.dto.enums.RoomType;
import com.nortal.healthcare.gateway.schedule.dto.enums.SlotType;
import com.nortal.healthcare.gateway.schedule.service.ScheduleService;
import com.nortal.healthcare.gateway.system.dto.HumanNameDto;
import com.nortal.healthcare.gateway.system.dto.PatientDto;
import com.nortal.healthcare.gateway.system.dto.PractitionerDto;
import com.nortal.healthcare.gateway.system.service.SystemService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SurgeryScheduleSlotControllerTest {

  private static final UUID RESOURCE_UUID = UUID.randomUUID();
  private static final UUID PATIENT1 = UUID.randomUUID();
  private static final UUID PATIENT2 = UUID.randomUUID();
  private static final UUID PRACTITIONER_UUID = UUID.randomUUID();

  private static final ZonedDateTime FROM = ZonedDateTime.now();
  private static final ZonedDateTime TO = FROM.plus(1, ChronoUnit.DAYS);

  @Mock
  private ScheduleService scheduleService;

  @Mock
  private PractitionerDto practitionerDto;

  @Mock
  private SystemService systemService;

  @Mock
  private ClinicalCareService clinicalCareService;

  @InjectMocks
  private SurgeryScheduleSlotController surgeryScheduleSlotController;

  @Test
  public void findAllSurgerySlots() {
    final UUID locationUuid1 = UUID.randomUUID();
    final UUID locationUuid2 = UUID.randomUUID();
    final UUID surgeryAppointUuid1 = UUID.randomUUID();
    final UUID surgeryAppointUuid2 = UUID.randomUUID();
    final UUID appointUuid1 = UUID.randomUUID();
    final UUID appointUuid2 = UUID.randomUUID();
    final TimeSlotDto timeSlotDto1 = getTimeSlotDtoWithResource(locationUuid1, ResourceType.LOCATION);
    final TimeSlotDto timeSlotDto2 = getTimeSlotDtoWithResource(locationUuid2, ResourceType.LOCATION);
    final TimeSlotDto timeSlotDto3 = getTimeSlotDtoWithResource(locationUuid2, ResourceType.LOCATION);
    timeSlotDto3.setChildrenResourceDtos(
        Set.of(TestDataUtil.getSchedulableResource(ResourceType.HEALTHCARE_PRACTITIONER, PRACTITIONER_UUID)));
    final SurgeryAppointmentDto surgeryAppointmentDto1 = createSurgeryAppointment(surgeryAppointUuid1);
    final SurgeryAppointmentDto surgeryAppointmentDto2 = createSurgeryAppointment(surgeryAppointUuid2);

    final Location location1 = new Location();
    location1.setId(locationUuid1.toString());
    final Location location2 = new Location();
    location2.setId(locationUuid2.toString());

    ResourceDto<AppointmentDto> resourceAppointmentDto1 =
        createResourceAppointmentDto(appointUuid1, surgeryAppointUuid1, timeSlotDto2.getId());
    timeSlotDto2.setAppointmentId(resourceAppointmentDto1.getResource().getId());
    ResourceDto<AppointmentDto> resourceAppointmentDto2 =
        createResourceAppointmentDto(appointUuid2, surgeryAppointUuid2, timeSlotDto1.getId());
    timeSlotDto1.setAppointmentId(resourceAppointmentDto2.getResource().getId());

    when(clinicalCareService.listEncounters(any())).thenReturn(
        Mono.just(List.of(getEncounter(resourceAppointmentDto1.getResource()))));
    when(practitionerDto.getUuid()).thenReturn(PRACTITIONER_UUID);
    when(scheduleService.listSurgeryTimeSlots(eq(List.of(locationUuid1, locationUuid2)), eq(FROM), eq(TO),
        eq(List.of(SlotType.SURGERY))
    )).thenReturn(
        Flux.fromIterable(Arrays.asList(timeSlotDto1, timeSlotDto2, timeSlotDto3)));
    when(
        systemService.getLocations(eq(RESOURCE_UUID), eq(List.of(RoomType.OPERATION, RoomType.RECOVERY)))).thenReturn(
        Mono.just(List.of(location1, location2)));
    when(scheduleService.listAppointmentsByResources(List.of(locationUuid1, locationUuid2), FROM, TO)).thenReturn(
        Flux.fromIterable(List.of(resourceAppointmentDto1, resourceAppointmentDto2)));
    when(systemService.getPatients(Set.of(PATIENT1, PATIENT2))).thenReturn(
        Flux.just(createPatientDto(PATIENT1), createPatientDto(PATIENT2)));
    when(clinicalCareService.getSurgeryAppointments(Set.of(surgeryAppointUuid1, surgeryAppointUuid2))).thenReturn(
        Mono.just(List.of(surgeryAppointmentDto1, surgeryAppointmentDto2)));
    when(systemService.getPractitioners(Set.of(PRACTITIONER_UUID))).thenReturn(Flux.just(practitionerDto));
    when(clinicalCareService.getPatientsRiskFacets(Set.of(PATIENT1, PATIENT2))).thenReturn(Mono.just(patientFacets()));
    when(clinicalCareService.searchTimestamps(anySet(),
        eq(ClinicalCareService.TIMESTAMP_DEFINITION_SURGERY_APPOINTMENT)
    )).thenReturn(Mono.just(List.of()));
    final Mono<List<LocationSurgerySlotsDto>> surgerySlotsMono =
        surgeryScheduleSlotController.findAllSurgerySlots(RESOURCE_UUID, FROM, TO);

    final List<LocationSurgerySlotsDto> surgerySlots = surgerySlotsMono.block();
    assertEquals(2, surgerySlots.size());
    assertEquals(1, surgerySlots.stream()
        .filter(sl -> sl.getLocation().equals(locationUuid1))
        .findFirst()
        .get()
        .getSurgerySlots()
        .size());

    final List<SurgerySlotDto> location2Slots = surgerySlots.stream()
        .filter(sl -> sl.getLocation().equals(locationUuid2))
        .findFirst()
        .get()
        .getSurgerySlots();
    assertEquals(practitionerDto, location2Slots.get(1).getPractitioners().iterator().next());

    final SurgerySlotDto location2Slots0 = surgerySlots.stream()
        .filter(sl -> sl.getLocation().equals(locationUuid2))
        .findFirst()
        .get()
        .getSurgerySlots()
        .get(0);

    final SurgerySlotDto location1Slots0 = surgerySlots.stream()
        .filter(sl -> sl.getLocation().equals(locationUuid1))
        .findFirst()
        .get()
        .getSurgerySlots()
        .get(0);

    assertAppointmentData(surgeryAppointUuid1, location2Slots0);
    assertAppointmentData(surgeryAppointUuid2, location1Slots0);

    // Each endpoint should be called only once
    verify(clinicalCareService, times(1)).listEncounters(any());
    verify(scheduleService, times(1)).listSurgeryTimeSlots(any(), any(), any(), any());
    verify(systemService, times(1)).getLocations(any(), any());
    verify(scheduleService, times(1)).listAppointmentsByResources(any(), any(), any());
    verify(systemService, times(1)).getPatients(any());
    verify(clinicalCareService, times(1)).getSurgeryAppointments(any());
    verify(systemService, times(1)).getPractitioners(any());
    verify(clinicalCareService, times(1)).getPatientsRiskFacets(any());
    verify(clinicalCareService, times(1)).searchTimestamps(any(), any());
  }

  private void assertAppointmentData(final UUID surgeryAppointUuid, final SurgerySlotDto locationSlot) {
    assertNotNull(locationSlot.getResourceAppointmentDto());
    assertEquals(1, locationSlot.getPatientFacets().size());
    assertEquals(2, locationSlot.getPatientFacets().get(PATIENT1).getRiskLevel().size());
    assertEquals(surgeryAppointUuid, locationSlot.getSurgeryAppointment().getUuid());
  }

  private Encounter getEncounter(final AppointmentDto appointmentDto) {
    final Encounter encounter = new Encounter();
    final Reference appointmentReference = new Reference();
    appointmentReference.setReference(appointmentDto.getUuid().toString(), "appointment");
    encounter.setAppointment(Set.of(appointmentReference));
    return encounter;
  }

  private Map<UUID, FacetDto> patientFacets() {
    final Map<UUID, FacetDto> patientFacets = new HashMap<>();

    final FacetDto patient1Facet = new FacetDto();
    patient1Facet.setRiskLevel(new ArrayList<>());
    patient1Facet.getRiskLevel().add(new RiskLevel(13, "1"));
    patient1Facet.getRiskLevel().add(new RiskLevel(9, "2"));
    patientFacets.put(PATIENT1, patient1Facet);

    return patientFacets;
  }

  private TimeSlotDto getTimeSlotDtoWithResource(final UUID resourceUuid, final ResourceType type) {
    final TimeSlotDto timeSlotDto = getTimeSlotDto();
    final SchedulableResourceDto schedulableResourceDto = TestDataUtil.getSchedulableResource(type, resourceUuid);
    timeSlotDto.setResourceDto(schedulableResourceDto);
    return timeSlotDto;
  }

  private TimeSlotDto getTimeSlotDto() {
    final TimeSlotDto timeSlotDto = new TimeSlotDto();
    timeSlotDto.setStartDate(ZonedDateTime.now());
    timeSlotDto.setEndDate(ZonedDateTime.now().plusMinutes(10));
    timeSlotDto.setId(new Random().nextLong());
    timeSlotDto.setDefaultAppointmentLength(15);
    return timeSlotDto;
  }

  private SurgeryAppointmentDto createSurgeryAppointment(final UUID surgeryAppointUuid) {
    final SurgeryAppointmentDto dto = new SurgeryAppointmentDto();
    dto.setAppointmentInfo(new AppointmentInfoDto());
    dto.getAppointmentInfo().setPerformer(PRACTITIONER_UUID);
    dto.setUuid(surgeryAppointUuid);
    return dto;
  }

  private PatientDto createPatientDto(final UUID patientUuid) {
    final PatientDto patientDto = new PatientDto();
    patientDto.setUuid(patientUuid);
    final HumanNameDto nameDto = new HumanNameDto();
    nameDto.setGiven("given name for " + patientDto);
    nameDto.setFamily("family name for " + patientDto);
    patientDto.setName(nameDto);
    return patientDto;
  }

  private ResourceDto<AppointmentDto> createResourceAppointmentDto(final UUID appointUuid,
      final UUID surgeryAppointment, final Long... timeslotIds) {
    final long appointmentId = new Random().nextLong();
    final ResourceDto<AppointmentDto> resourceDto = new ResourceDto<>();
    final AppointmentDto dto = new AppointmentDto();
    dto.setUuid(appointUuid);
    dto.setType(AppointmentType.PERSONAL);
    dto.setTimeSlots(List.of(timeslotIds));
    dto.setPatients(Set.of(PATIENT1, PATIENT2));
    dto.setSurgeryAppointment(surgeryAppointment);
    dto.setId(appointmentId);
    resourceDto.setResource(dto);
    return resourceDto;
  }

}

package com.ai.e2e.tx.example.gateway.schedule.api.custom;

import com.nortal.healthcare.commons.fhir.dto.resource.Encounter;
import com.nortal.healthcare.commons.fhir.dto.resource.Location;
import com.nortal.healthcare.gateway.clinical.care.service.ClinicalCareService;
import com.nortal.healthcare.gateway.common.dto.ResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.AppointmentDto;
import com.nortal.healthcare.gateway.schedule.dto.HealthcareServiceDto;
import com.nortal.healthcare.gateway.schedule.dto.HealthcareServiceSlotsDto;
import com.nortal.healthcare.gateway.schedule.dto.TimeSlotDto;
import com.nortal.healthcare.gateway.schedule.dto.enums.ResourceType;
import com.nortal.healthcare.gateway.schedule.dto.enums.SlotType;
import com.nortal.healthcare.gateway.schedule.service.ScheduleService;
import com.nortal.healthcare.gateway.system.dto.PatientDto;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LaboratoryScheduleSlotControllerTest {

  private static final UUID LOCATION_UUID = UUID.randomUUID();
  private static final UUID PATIENT_UUID = UUID.randomUUID();
  private static final UUID HEALTHCARE_SERVICE_UUID = UUID.randomUUID();
  private static final UUID APPOINTMENT_UUID = UUID.randomUUID();
  private static final ZonedDateTime FROM = ZonedDateTime.now();
  private static final ZonedDateTime TO = FROM.plus(1, ChronoUnit.DAYS);

  @Mock
  private ScheduleService scheduleService;

  @Mock
  private SystemService systemService;

  @Mock
  private ClinicalCareService clinicalCareService;

  @InjectMocks
  private LaboratoryScheduleSlotController laboratoryScheduleSlotController;

  @Test
  public void findAllNoTimeSlotsFound() {
    when(scheduleService.listTimeSlotsByParentLocation(eq(LOCATION_UUID), eq(FROM), eq(TO),
        eq(List.of(SlotType.LABORATORY))
    )).thenReturn(
        Flux.fromIterable(Arrays.asList()));

    final Mono<List<HealthcareServiceSlotsDto>> healthcareSlots =
        laboratoryScheduleSlotController.findAllLabSlotsBasedOnLocation(LOCATION_UUID, FROM, TO);

    assertTrue(healthcareSlots.block().isEmpty());
  }

  @Test
  public void findAllLaboratorySlots() {
    final TimeSlotDto timeSlotDto1 =
        TestDataUtil.getTimeSlotDto(HEALTHCARE_SERVICE_UUID, ResourceType.HEALTHCARE_SERVICE, LOCATION_UUID);
    final TimeSlotDto timeSlotDto2 =
        TestDataUtil.getTimeSlotDto(HEALTHCARE_SERVICE_UUID, ResourceType.HEALTHCARE_SERVICE, LOCATION_UUID);
    timeSlotDto2.setChildrenResourceDtos(
        Set.of(TestDataUtil.getSchedulableResource(ResourceType.LOCATION, LOCATION_UUID)));
    final PatientDto patientDto = TestDataUtil.getPatientDto(PATIENT_UUID);
    final HealthcareServiceDto healthcareServiceDto = TestDataUtil.getHealthcareServiceDto(HEALTHCARE_SERVICE_UUID);
    final ResourceDto<AppointmentDto> appointmentDto =
        TestDataUtil.getAppointmentDto(timeSlotDto1, patientDto, APPOINTMENT_UUID);
    final Encounter encounter = TestDataUtil.getEncounter(appointmentDto.getResource());
    final Location location = new Location();
    location.setId(LOCATION_UUID.toString());

    when(scheduleService.listTimeSlotsByParentLocation(LOCATION_UUID, FROM, TO, List.of(SlotType.LABORATORY)))
        .thenReturn(Flux.fromIterable(Arrays.asList(timeSlotDto1, timeSlotDto2)));
    when(systemService.getPatients(Set.of(PATIENT_UUID)))
        .thenReturn(Flux.fromIterable(Arrays.asList(patientDto)));
    when(systemService.getLocation(LOCATION_UUID))
        .thenReturn(Mono.just(location));
    when(clinicalCareService.listEncounters(Set.of(APPOINTMENT_UUID)))
        .thenReturn(Mono.just(List.of(encounter)));
    when(systemService.getHealthcareServices(List.of(HEALTHCARE_SERVICE_UUID)))
        .thenReturn(Flux.fromIterable(Arrays.asList(healthcareServiceDto)));
    when(clinicalCareService.listEncounters(Set.of(appointmentDto.getResource().getUuid())))
        .thenReturn(Mono.just(Arrays.asList(encounter)));
    when(scheduleService.listAppointmentsByResources(List.of(HEALTHCARE_SERVICE_UUID), FROM, TO))
        .thenReturn(Flux.fromIterable(Arrays.asList(appointmentDto)));

    final Mono<List<HealthcareServiceSlotsDto>> healthcareSlotsMono =
        laboratoryScheduleSlotController.findAllLabSlotsBasedOnLocation(LOCATION_UUID, FROM, TO);

    final List<HealthcareServiceSlotsDto> healthcareServiceSlotDtos = healthcareSlotsMono.block();
    assertEquals(1, healthcareServiceSlotDtos.size());
    assertEquals(2, healthcareServiceSlotDtos.stream()
        .filter(hs -> hs.getHealthcareService().getUuid().equals(HEALTHCARE_SERVICE_UUID))
        .findFirst()
        .get()
        .getSlots()
        .size());

    assertEquals(encounter, healthcareServiceSlotDtos.stream()
        .flatMap(hs -> hs.getSlots().stream())
        .findFirst()
        .get()
        .getEncounter());

    assertEquals(List.of(location), healthcareServiceSlotDtos.stream()
        .flatMap(hs -> hs.getSlots().stream())
        .map(hs -> hs.getLocations())
        .filter(locations -> !locations.isEmpty())
        .findFirst()
        .get());
  }

}

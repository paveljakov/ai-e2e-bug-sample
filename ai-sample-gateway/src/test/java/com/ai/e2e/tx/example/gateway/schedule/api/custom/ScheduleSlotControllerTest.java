package com.ai.e2e.tx.example.gateway.schedule.api.custom;

import com.nortal.healthcare.commons.fhir.dto.Reference;
import com.nortal.healthcare.commons.fhir.dto.resource.Encounter;
import com.nortal.healthcare.commons.fhir.dto.resource.Location;
import com.nortal.healthcare.gateway.clinical.care.service.ClinicalCareService;
import com.nortal.healthcare.gateway.common.dto.ResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.AppointmentDto;
import com.nortal.healthcare.gateway.schedule.dto.LocationScheduleSlotDto;
import com.nortal.healthcare.gateway.schedule.dto.SchedulableResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.TimeSlotDto;
import com.nortal.healthcare.gateway.schedule.dto.enums.ResourceType;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleSlotControllerTest {

  private static final UUID RESOURCE_UUID = UUID.randomUUID();

  private static final ZonedDateTime FROM = ZonedDateTime.now();

  private static final ZonedDateTime TO = FROM.plus(1, ChronoUnit.DAYS);

  @Mock
  private ScheduleService scheduleService;

  @Mock
  private SystemService systemService;

  @Mock
  private ClinicalCareService clinicalCareService;

  @InjectMocks
  private ScheduleSlotController scheduleSlotController;

  @Test
  public void findAllNoTimeSlotsFound() {
    when(scheduleService.listTimeSlots(eq(List.of(RESOURCE_UUID)), eq(FROM), eq(TO))).thenReturn(
        Flux.fromIterable(Arrays.asList()));

    final Mono<List<LocationScheduleSlotDto>> scheduleSlotDtoListMono =
        scheduleSlotController.findAll(RESOURCE_UUID, FROM, TO);

    assertTrue(scheduleSlotDtoListMono.block().isEmpty());
  }

  @Test
  public void findAll() {
    final UUID locationUuid = UUID.randomUUID();
    final TimeSlotDto timeSlotDto =
        getTimeSlotDtoWithChild(RESOURCE_UUID, ResourceType.HEALTHCARE_PRACTITIONER, locationUuid,
            ResourceType.LOCATION
        );
    final PatientDto patientDto = TestDataUtil.getPatientDto(UUID.randomUUID());
    final ResourceDto<AppointmentDto> appointmentDto =
        TestDataUtil.getAppointmentDto(timeSlotDto, patientDto, UUID.randomUUID());
    final Location clinicLocation = getLocation();
    final Location location = getLocation(clinicLocation, locationUuid);
    final Encounter encounter = TestDataUtil.getEncounter(appointmentDto.getResource());

    final UUID freeLocationUuid = UUID.randomUUID();
    final TimeSlotDto freeTimeSlotDto =
        getTimeSlotDtoWithChild(RESOURCE_UUID, ResourceType.HEALTHCARE_PRACTITIONER, freeLocationUuid,
            ResourceType.LOCATION
        );
    final Location freeClinicLocation = getLocation();
    final Location freeLocation = getLocation(freeClinicLocation, freeLocationUuid);

    when(scheduleService.listTimeSlots(eq(List.of(RESOURCE_UUID)), eq(FROM), eq(TO))).thenReturn(
        Flux.fromIterable(Arrays.asList(timeSlotDto, freeTimeSlotDto)));
    when(scheduleService.listResourceDtoAppointments(eq(RESOURCE_UUID), eq(FROM), eq(TO))).thenReturn(
        Flux.fromIterable((Arrays.asList(appointmentDto))));
    when(systemService.getLocation(eq(getLocationUuid(timeSlotDto)))).thenReturn(Mono.just(location));
    when(systemService.getLocation(eq(getLocationUuid(freeTimeSlotDto)))).thenReturn(Mono.just(freeLocation));
    when(systemService.getPatients(eq(Set.of(patientDto.getUuid())))).thenReturn(
        Flux.fromIterable(Arrays.asList(patientDto)));
    when(systemService.getLocation(UUID.fromString(clinicLocation.getId()))).thenReturn(Mono.just(clinicLocation));
    when(systemService.getLocation(UUID.fromString(freeClinicLocation.getId()))).thenReturn(
        Mono.just(freeClinicLocation));
    when(clinicalCareService.listEncounters(Set.of(appointmentDto.getResource().getUuid()))).thenReturn(
        Mono.just(Arrays.asList(encounter)));

    final Mono<List<LocationScheduleSlotDto>> scheduleSlotDtoListMono =
        scheduleSlotController.findAll(RESOURCE_UUID, FROM, TO);

    final List<LocationScheduleSlotDto> locationScheduleSlotDtos = scheduleSlotDtoListMono.block();
    assertEquals(2, locationScheduleSlotDtos.size());
    assertBookedSlot(locationScheduleSlotDtos, timeSlotDto, patientDto, appointmentDto, location, encounter);
    assertNonBookedSlot(locationScheduleSlotDtos, freeTimeSlotDto, freeLocation);
  }

  private void assertNonBookedSlot(final List<LocationScheduleSlotDto> locationScheduleSlotDtos,
      final TimeSlotDto freeTimeSlotDto,
      final Location freeLocation) {
    final Optional<LocationScheduleSlotDto> nonBookedScheduleSlotOptional =
        locationScheduleSlotDtos.stream()
            .filter(locationScheduleSlotDto -> locationScheduleSlotDto.getResourceAppointmentDto() == null)
            .findFirst();
    assertTrue("Free schedule slot was not found", nonBookedScheduleSlotOptional.isPresent());

    final LocationScheduleSlotDto nonBookedScheduleSlot = nonBookedScheduleSlotOptional.get();
    assertEquals(Arrays.asList(freeTimeSlotDto), nonBookedScheduleSlot.getTimeSlots());
    assertEquals(Arrays.asList(freeLocation), nonBookedScheduleSlot.getLocations());
  }

  private void assertBookedSlot(final List<LocationScheduleSlotDto> locationScheduleSlotDtos,
      final TimeSlotDto timeSlotDto,
      final PatientDto patientDto, final ResourceDto<AppointmentDto> appointmentDto, final Location location,
      final Encounter encounter) {
    final Optional<LocationScheduleSlotDto> bookedScheduleSlotOptional =
        locationScheduleSlotDtos.stream()
            .filter(locationScheduleSlotDto -> locationScheduleSlotDto.getResourceAppointmentDto() != null)
            .findFirst();
    assertTrue("Booked schedule slot was not found", bookedScheduleSlotOptional.isPresent());

    final LocationScheduleSlotDto bookedScheduleSlot = bookedScheduleSlotOptional.get();
    assertEquals(Arrays.asList(timeSlotDto), bookedScheduleSlot.getTimeSlots());
    assertEquals(Arrays.asList(patientDto), bookedScheduleSlot.getPatients());
    assertEquals(appointmentDto, bookedScheduleSlot.getResourceAppointmentDto());
    assertEquals(Arrays.asList(location), bookedScheduleSlot.getLocations());
    assertEquals(encounter, bookedScheduleSlot.getEncounter());
  }

  private Location getLocation() {
    final Location location = new Location();
    location.setId(UUID.randomUUID().toString());
    return location;
  }

  private UUID getLocationUuid(final TimeSlotDto timeSlotDto) {
    return timeSlotDto.getChildrenResourceDtos()
        .stream()
        .filter(schedulableResourceDto -> schedulableResourceDto.getResourceTypeCode().equals(ResourceType.LOCATION))
        .map(SchedulableResourceDto::getUuid)
        .findFirst()
        .orElse(null);
  }

  private Location getLocation(final Location clinicLocation, final UUID locationUuid) {
    final Location location = new Location();
    final Reference reference = new Reference();
    location.setId(locationUuid.toString());
    reference.setReference("Location/" + clinicLocation.getId());
    location.setPartOf(reference);
    return location;
  }

  private TimeSlotDto getTimeSlotDtoWithChild(final UUID parentResourceUuid, final ResourceType parentType,
      final UUID resourceUuid, final ResourceType type) {
    final TimeSlotDto timeSlotDto = TestDataUtil.getTimeSlotDto(parentResourceUuid, parentType, UUID.randomUUID());
    final SchedulableResourceDto schedulableResourceDto = getSchedulableResource(type, resourceUuid);
    timeSlotDto.setChildrenResourceDtos(Set.of(schedulableResourceDto));
    return timeSlotDto;
  }

  private SchedulableResourceDto getSchedulableResource(final ResourceType resourceType, final UUID uuid) {
    final SchedulableResourceDto schedulableResourceDto = new SchedulableResourceDto();
    schedulableResourceDto.setResourceTypeCode(resourceType);
    schedulableResourceDto.setUuid(uuid);
    return schedulableResourceDto;
  }

}

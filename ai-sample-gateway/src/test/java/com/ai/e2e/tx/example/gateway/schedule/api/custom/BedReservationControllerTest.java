package com.ai.e2e.tx.example.gateway.schedule.api.custom;

import com.nortal.healthcare.commons.fhir.dto.resource.Location;
import com.nortal.healthcare.gateway.clinical.care.dto.ProcedureWithSideDto;
import com.nortal.healthcare.gateway.clinical.care.dto.surgery.appointment.ProcedurePackageUuidDto;
import com.nortal.healthcare.gateway.clinical.care.dto.surgery.appointment.SurgeryAppointmentWardSummaryDto;
import com.nortal.healthcare.gateway.clinical.care.service.ClinicalCareService;
import com.nortal.healthcare.gateway.schedule.dto.BedReservationAggregatedDto;
import com.nortal.healthcare.gateway.schedule.dto.BedReservationDto;
import com.nortal.healthcare.gateway.schedule.dto.enums.RoomType;
import com.nortal.healthcare.gateway.schedule.service.ScheduleService;
import com.nortal.healthcare.gateway.system.dto.PatientDto;
import com.nortal.healthcare.gateway.system.service.SystemService;
import com.nortal.healthcare.utils.LocalDateTimeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BedReservationControllerTest {
  private ZoneId ZONE_ID = ZoneId.of("Europe/Helsinki");
  private LocalDate LOCAL_DATE = LocalDate.of(2020, Month.AUGUST, 21);
  private ZonedDateTime START = LocalDateTimeUtils.atStartOfTheDay(LOCAL_DATE, ZONE_ID).atZone(ZoneOffset.UTC);
  private ZonedDateTime END = LocalDateTimeUtils.atEndOfTheDay(LOCAL_DATE, ZONE_ID).atZone(ZoneOffset.UTC);
  private static final UUID LOCATION_CLINIC = UUID.randomUUID();
  private static final List<UUID> LOCATIONS_BED = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  private static final List<UUID> PATIENTS = List.of(UUID.randomUUID(), UUID.randomUUID());
  private static final List<UUID> SURGERY_APPOINTMENTS = List.of(UUID.randomUUID(), UUID.randomUUID());

  @Mock
  private ScheduleService scheduleService;
  @Mock
  private SystemService systemService;
  @Mock
  private ClinicalCareService clinicalCareService;
  @InjectMocks
  private BedReservationController bedReservationController;

  @Test
  public void findBedReservations_noBeds() {
    assertNotNull(systemService);
    when(systemService.getLocations(eq(LOCATION_CLINIC), eq(List.of(RoomType.BED)))).thenReturn(Mono.just(List.of()));
    final Mono<Collection<BedReservationAggregatedDto>> bedReservationsMono =
        bedReservationController.findBedReservations(LOCATION_CLINIC, START, END);
    final Collection<BedReservationAggregatedDto> aggregatedDtos = bedReservationsMono.block();
    assertTrue(aggregatedDtos.isEmpty());
  }

  @Test
  public void findBedReservations_noReservations() {
    when(systemService.getLocations(eq(LOCATION_CLINIC), eq(List.of(RoomType.BED)))).thenReturn(Mono.just((getBeds())));
    when(scheduleService.listBedReservations(eq(LOCATIONS_BED), eq(START), eq(END))).thenReturn(Flux.empty());
    final Mono<Collection<BedReservationAggregatedDto>> bedReservationsMono =
        bedReservationController.findBedReservations(LOCATION_CLINIC, START, END);
    final Collection<BedReservationAggregatedDto> aggregatedDtos = bedReservationsMono.block();
    assertTrue(aggregatedDtos.isEmpty());
  }

  @Test
  public void findBedReservations_reservations() {
    final LocalDateTime start = LocalDateTimeUtils.atStartOfTheDay(LOCAL_DATE, ZONE_ID);
    final LocalDateTime end = LocalDateTimeUtils.atEndOfTheDay(LOCAL_DATE, ZONE_ID);
    when(systemService.getLocations(eq(LOCATION_CLINIC), eq(List.of(RoomType.BED)))).thenReturn(Mono.just((getBeds())));
    when(scheduleService.listBedReservations(eq(LOCATIONS_BED), eq(START), eq(END))).thenReturn(
        Flux.fromStream(this::getBedReservationsStream));
    when(systemService.getPatients(Set.copyOf(PATIENTS))).thenReturn(Flux.fromStream(this::getPatientsStream));
    when(clinicalCareService.getSurgeryAppointmentWardSummaries(Set.copyOf(SURGERY_APPOINTMENTS))).thenReturn(
        Mono.just(getSurgeryAppointmentWardSummaries()));
    when(clinicalCareService.getProcedurePackageUuidsBySurgeryAppointmentUuid(Set.copyOf(SURGERY_APPOINTMENTS)))
        .thenReturn(Mono.just(getProcedurePackageUuidDtos()));
    final Mono<Collection<BedReservationAggregatedDto>> bedReservationsMono =
        bedReservationController.findBedReservations(LOCATION_CLINIC, START, END);
    final Collection<BedReservationAggregatedDto> aggregatedDtos = bedReservationsMono.block();
    assertEquals(2, aggregatedDtos.size());
    PATIENTS.forEach(patientUuid ->
        assertTrue(aggregatedDtos.stream().anyMatch(dto -> dto.getPatient().getUuid().equals(patientUuid))));
    SURGERY_APPOINTMENTS.forEach(surgeryAppointmentUuid ->
        assertTrue(
            aggregatedDtos.stream().anyMatch(dto -> dto.getSurgeryAppointment().equals(surgeryAppointmentUuid))));
  }

  @Test
  public void findBedReservations_reservations_without_surgeryAppointment() {
    final LocalDateTime start = LocalDateTimeUtils.atStartOfTheDay(LOCAL_DATE, ZONE_ID);
    final LocalDateTime end = LocalDateTimeUtils.atEndOfTheDay(LOCAL_DATE, ZONE_ID);
    when(systemService.getLocations(eq(LOCATION_CLINIC), eq(List.of(RoomType.BED)))).thenReturn(Mono.just((getBeds())));
    when(scheduleService.listBedReservations(eq(LOCATIONS_BED), eq(START), eq(END))).thenReturn(
        Flux.fromStream(this::getBedReservationsStreamWithoutSurgeryAppointment));
    when(systemService.getPatients(Set.copyOf(PATIENTS))).thenReturn(Flux.fromStream(this::getPatientsStream));
    final Mono<Collection<BedReservationAggregatedDto>> bedReservationsMono =
        bedReservationController.findBedReservations(LOCATION_CLINIC, START, END);
    final Collection<BedReservationAggregatedDto> aggregatedDtos = bedReservationsMono.block();
    assertEquals(2, aggregatedDtos.size());
    PATIENTS.forEach(patientUuid ->
        assertTrue(aggregatedDtos.stream().anyMatch(dto -> dto.getPatient().getUuid().equals(patientUuid))));
    assertTrue(aggregatedDtos.stream().anyMatch(dto -> dto.getSurgeryAppointment() == null));
  }

  private List<Location> getBeds() {
    return LOCATIONS_BED.stream().map(uuid -> getBed(uuid)).collect(Collectors.toList());
  }

  private Location getBed(final UUID uuid) {
    final Location location = new Location();
    location.setId(uuid.toString());
    location.setName(uuid.toString().substring(0, 5));
    return location;
  }

  private Stream<BedReservationDto> getBedReservationsStream() {
    return Arrays.stream(new BedReservationDto[] {
        getBedReservation(LOCATIONS_BED.get(0), PATIENTS.get(0), SURGERY_APPOINTMENTS.get(0)),
        getBedReservation(LOCATIONS_BED.get(1), PATIENTS.get(1), SURGERY_APPOINTMENTS.get(1))
    });
  }

  private BedReservationDto getBedReservation(final UUID bed, final UUID patient, final UUID surgeryAppointment) {
    final BedReservationDto bedReservationDto = new BedReservationDto();
    bedReservationDto.setUuid(UUID.randomUUID());
    bedReservationDto.setBed(bed);
    bedReservationDto.setPatient(patient);
    bedReservationDto.setSurgeryAppointment(surgeryAppointment);
    return bedReservationDto;
  }

  private Stream<PatientDto> getPatientsStream() {
    return PATIENTS.stream().map(this::getPatient);
  }

  private PatientDto getPatient(final UUID uuid) {
    final PatientDto patientDto = new PatientDto();
    patientDto.setUuid(uuid);
    return patientDto;
  }

  private Collection<SurgeryAppointmentWardSummaryDto> getSurgeryAppointmentWardSummaries() {
    return SURGERY_APPOINTMENTS.stream().map(this::getSurgeryAppointmentWardSummary).collect(Collectors.toList());
  }

  private SurgeryAppointmentWardSummaryDto getSurgeryAppointmentWardSummary(final UUID uuid) {
    final SurgeryAppointmentWardSummaryDto dto = new SurgeryAppointmentWardSummaryDto();
    dto.setSurgeryAppointment(uuid);
    dto.setMainProcedure(getProcedureWithSide());
    return dto;
  }

  private ProcedureWithSideDto getProcedureWithSide() {
    final ProcedureWithSideDto dto = new ProcedureWithSideDto();
    return dto;
  }

  private Stream<BedReservationDto> getBedReservationsStreamWithoutSurgeryAppointment() {
    return Arrays.stream(new BedReservationDto[] {
        getBedReservation(LOCATIONS_BED.get(0), PATIENTS.get(0), null),
        getBedReservation(LOCATIONS_BED.get(1), PATIENTS.get(1), null)
    });
  }

  private Collection<ProcedurePackageUuidDto> getProcedurePackageUuidDtos() {
    return SURGERY_APPOINTMENTS.stream().map(this::getProcedurePackageUuidDto).collect(Collectors.toList());
  }

  private ProcedurePackageUuidDto getProcedurePackageUuidDto(final UUID uuid) {
    final ProcedurePackageUuidDto dto = new ProcedurePackageUuidDto();
    dto.setSurgeryAppointment(uuid);
    dto.setProcedurePackage(UUID.randomUUID());
    return dto;
  }
}

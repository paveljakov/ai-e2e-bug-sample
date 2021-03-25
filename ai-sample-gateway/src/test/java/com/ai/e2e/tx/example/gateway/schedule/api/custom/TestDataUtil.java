package com.ai.e2e.tx.example.gateway.schedule.api.custom;

import com.nortal.healthcare.commons.fhir.dto.Reference;
import com.nortal.healthcare.commons.fhir.dto.resource.Encounter;
import com.nortal.healthcare.gateway.common.dto.ResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.AppointmentDto;
import com.nortal.healthcare.gateway.schedule.dto.HealthcareServiceDto;
import com.nortal.healthcare.gateway.schedule.dto.SchedulableResourceDto;
import com.nortal.healthcare.gateway.schedule.dto.TimeSlotDto;
import com.nortal.healthcare.gateway.schedule.dto.enums.ResourceType;
import com.nortal.healthcare.gateway.system.dto.PatientDto;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class TestDataUtil {

  protected static PatientDto getPatientDto(final UUID patientUuid) {
    final PatientDto patientDto = new PatientDto();
    patientDto.setUuid(patientUuid);
    return patientDto;
  }

  public static HealthcareServiceDto getHealthcareServiceDto(final UUID healthcareServiceUuid) {
    final HealthcareServiceDto healthcareServiceDto = new HealthcareServiceDto();
    healthcareServiceDto.setUuid(healthcareServiceUuid);
    return healthcareServiceDto;
  }

  public static ResourceDto<AppointmentDto> getAppointmentDto(final TimeSlotDto timeSlotDto,
      final PatientDto patientDto, final UUID appoitnmentUuid) {
    final long appointmentId = new Random().nextLong();
    final ResourceDto<AppointmentDto> resourceDto = new ResourceDto<>();
    timeSlotDto.setAppointmentId(appointmentId);
    final AppointmentDto appointmentDto = new AppointmentDto();
    appointmentDto.setTimeSlots(Arrays.asList(timeSlotDto.getId()));
    appointmentDto.setPatients(Set.of(patientDto.getUuid()));
    appointmentDto.setUuid(appoitnmentUuid);
    appointmentDto.setId(appointmentId);
    resourceDto.setResource(appointmentDto);
    return resourceDto;
  }

  public static Encounter getEncounter(final AppointmentDto appointmentDto) {
    final Encounter encounter = new Encounter();
    final Reference appointmentReference = new Reference();
    appointmentReference.setReference(appointmentDto.getUuid().toString(), "appointment");
    encounter.setAppointment(Set.of(appointmentReference));
    return encounter;
  }

  public static TimeSlotDto getTimeSlotDto(final UUID resourceUuid, final ResourceType type, final UUID locationUuid) {
    final TimeSlotDto timeSlotDto = new TimeSlotDto();
    timeSlotDto.setStartDate(ZonedDateTime.now());
    timeSlotDto.setEndDate(ZonedDateTime.now().plusMinutes(10));
    timeSlotDto.setId(new Random().nextLong());
    timeSlotDto.setDefaultAppointmentLength(15);
    timeSlotDto.setResourceDto(getSchedulableResource(type, resourceUuid));
    timeSlotDto.setClinicLocation(locationUuid);
    return timeSlotDto;
  }

  public static SchedulableResourceDto getSchedulableResource(final ResourceType resourceType, final UUID uuid) {
    final SchedulableResourceDto schedulableResourceDto = new SchedulableResourceDto();
    schedulableResourceDto.setResourceTypeCode(resourceType);
    schedulableResourceDto.setUuid(uuid);
    return schedulableResourceDto;
  }

}

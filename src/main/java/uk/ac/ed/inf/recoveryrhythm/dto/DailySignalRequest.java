package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailySignalRequest {

    private LocalDate logDate;

    private boolean morningCheckInCompleted;
    private boolean medicationTaken;
    private boolean mealLogged;
    private boolean activityLogged;
    private boolean appointmentScheduled;
    private boolean appointmentAttended;
    private boolean eveningCheckInCompleted;

    private Integer sleepStartHour;
    private String notes;
}

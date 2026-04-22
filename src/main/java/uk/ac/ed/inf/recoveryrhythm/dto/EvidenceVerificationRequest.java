package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Data;

@Data
public class EvidenceVerificationRequest {
    private String clinicianName;
    private String reason;
}

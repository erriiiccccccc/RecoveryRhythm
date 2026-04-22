package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PatientLoginAccountResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String password;
}

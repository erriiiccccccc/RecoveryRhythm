package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.EvidenceSignalType;
import uk.ac.ed.inf.recoveryrhythm.entity.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SignalEvidenceResponse {
    private UUID id;
    private UUID userId;
    private UUID signalLogId;
    private LocalDate logDate;
    private EvidenceSignalType signalType;
    private String mimeType;
    private String imageBase64;
    private VerificationStatus status;
    private String clinicianName;
    private String verificationReason;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
}

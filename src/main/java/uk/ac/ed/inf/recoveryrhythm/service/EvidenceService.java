package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ed.inf.recoveryrhythm.dto.SignalEvidenceResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.DailySignalLogRepository;
import uk.ac.ed.inf.recoveryrhythm.repository.SignalEvidenceRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {

    private final SignalEvidenceRepository evidenceRepo;
    private final DailySignalLogRepository signalRepo;
    private final UserService userService;
    private final KafkaEventPublisher kafkaPublisher;

    @Transactional
    public List<SignalEvidenceResponse> attachEvidenceToSignal(
            UUID userId,
            UUID signalLogId,
            Map<EvidenceSignalType, MultipartFile> filesByType
    ) {
        RecoveryUser user = userService.requireUser(userId);
        DailySignalLog logEntity = signalRepo.findById(signalLogId)
                .orElseThrow(() -> new IllegalArgumentException("Signal log not found: " + signalLogId));

        if (!logEntity.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Signal log does not belong to user");
        }

        List<SignalEvidence> created = new ArrayList<>();
        for (Map.Entry<EvidenceSignalType, MultipartFile> entry : filesByType.entrySet()) {
            MultipartFile file = entry.getValue();
            if (file == null || file.isEmpty()) {
                continue;
            }
            String mime = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
            if (!mime.startsWith("image/")) {
                throw new IllegalArgumentException("Only image uploads are supported for evidence");
            }
            try {
                SignalEvidence evidence = SignalEvidence.builder()
                        .user(user)
                        .dailySignalLog(logEntity)
                        .signalType(entry.getKey())
                        .mimeType(mime)
                        .imageData(file.getBytes())
                        .status(VerificationStatus.PENDING)
                        .build();
                created.add(evidenceRepo.save(evidence));
                kafkaPublisher.publishSignalLogged(userId, Map.of(
                        "eventType", "evidence.uploaded",
                        "signalType", entry.getKey().name(),
                        "logDate", logEntity.getLogDate().toString()
                ));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read uploaded file", e);
            }
        }

        refreshVerificationState(logEntity);
        signalRepo.save(logEntity);
        return created.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SignalEvidenceResponse verifyEvidence(UUID evidenceId, VerificationStatus status, String clinicianName, String reason) {
        if (status == VerificationStatus.PENDING) {
            throw new IllegalArgumentException("Verification action must be APPROVED or DENIED");
        }
        SignalEvidence evidence = evidenceRepo.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + evidenceId));
        evidence.setStatus(status);
        evidence.setClinicianName((clinicianName == null || clinicianName.isBlank()) ? "Clinician" : clinicianName.trim());
        evidence.setVerificationReason(reason);
        evidence.setVerifiedAt(java.time.LocalDateTime.now());
        evidence = evidenceRepo.save(evidence);

        refreshVerificationState(evidence.getDailySignalLog());
        signalRepo.save(evidence.getDailySignalLog());

        kafkaPublisher.publishSignalLogged(evidence.getUser().getId(), Map.of(
                "eventType", "evidence.verified",
                "status", status.name(),
                "signalType", evidence.getSignalType().name(),
                "logDate", evidence.getDailySignalLog().getLogDate().toString()
        ));
        return toResponse(evidence);
    }

    public List<SignalEvidenceResponse> getEvidenceForSignal(UUID signalLogId) {
        DailySignalLog signalLog = signalRepo.findById(signalLogId)
                .orElseThrow(() -> new IllegalArgumentException("Signal log not found: " + signalLogId));
        return evidenceRepo.findByDailySignalLogOrderByUploadedAtDesc(signalLog).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SignalEvidenceResponse> getPendingEvidenceQueue() {
        return evidenceRepo.findTop100ByStatusOrderByUploadedAtDesc(VerificationStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserEvidenceSummary summarizeUserEvidence(List<DailySignalLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return new UserEvidenceSummary(0, 0, 0);
        }
        List<SignalEvidence> evidence = evidenceRepo.findByDailySignalLogInOrderByUploadedAtDesc(logs);
        int pending = (int) evidence.stream().filter(e -> e.getStatus() == VerificationStatus.PENDING).count();
        int approved = (int) evidence.stream().filter(e -> e.getStatus() == VerificationStatus.APPROVED).count();
        int denied = (int) evidence.stream().filter(e -> e.getStatus() == VerificationStatus.DENIED).count();
        return new UserEvidenceSummary(pending, approved, denied);
    }

    public EvidenceRollup summarizeSignalEvidence(DailySignalLog signalLog) {
        List<SignalEvidence> evidence = evidenceRepo.findByDailySignalLogOrderByUploadedAtDesc(signalLog);
        int pending = (int) evidence.stream().filter(e -> e.getStatus() == VerificationStatus.PENDING).count();
        int denied = (int) evidence.stream().filter(e -> e.getStatus() == VerificationStatus.DENIED).count();
        return new EvidenceRollup(signalLog.getVerificationState(), pending, denied);
    }

    public boolean isSignalClaimApproved(DailySignalLog signalLog, EvidenceSignalType signalType) {
        return evidenceRepo.existsByDailySignalLogAndSignalTypeAndStatus(signalLog, signalType, VerificationStatus.APPROVED);
    }

    public long countDeniedEvidence(RecoveryUser user, java.time.LocalDate from, java.time.LocalDate to) {
        return evidenceRepo.countByUserAndStatusAndDailySignalLog_LogDateBetween(user, VerificationStatus.DENIED, from, to);
    }

    @Transactional
    public void refreshVerificationState(DailySignalLog signalLog) {
        Set<EvidenceSignalType> requiredClaims = new HashSet<>();
        if (signalLog.isMedicationTaken()) requiredClaims.add(EvidenceSignalType.MEDICATION);
        if (signalLog.isMealLogged()) requiredClaims.add(EvidenceSignalType.MEAL);
        if (signalLog.isActivityLogged()) requiredClaims.add(EvidenceSignalType.ACTIVITY);

        if (requiredClaims.isEmpty()) {
            signalLog.setVerificationState(DailyVerificationState.VERIFIED);
            return;
        }

        int approvedCount = 0;
        int pendingCount = 0;
        int deniedOrMissingCount = 0;
        for (EvidenceSignalType type : requiredClaims) {
            boolean approved = evidenceRepo.existsByDailySignalLogAndSignalTypeAndStatus(signalLog, type, VerificationStatus.APPROVED);
            boolean pending = evidenceRepo.existsByDailySignalLogAndSignalTypeAndStatus(signalLog, type, VerificationStatus.PENDING);
            boolean denied = evidenceRepo.existsByDailySignalLogAndSignalTypeAndStatus(signalLog, type, VerificationStatus.DENIED);
            if (approved) approvedCount++;
            else if (pending) pendingCount++;
            else if (denied) deniedOrMissingCount++;
            else deniedOrMissingCount++;
        }

        if (approvedCount == requiredClaims.size()) {
            signalLog.setVerificationState(DailyVerificationState.VERIFIED);
        } else if (approvedCount > 0) {
            signalLog.setVerificationState(DailyVerificationState.PARTIAL);
        } else if (pendingCount > 0 && deniedOrMissingCount == 0) {
            signalLog.setVerificationState(DailyVerificationState.PENDING);
        } else {
            signalLog.setVerificationState(DailyVerificationState.REJECTED);
        }
    }

    private SignalEvidenceResponse toResponse(SignalEvidence evidence) {
        return SignalEvidenceResponse.builder()
                .id(evidence.getId())
                .userId(evidence.getUser().getId())
                .signalLogId(evidence.getDailySignalLog().getId())
                .logDate(evidence.getDailySignalLog().getLogDate())
                .signalType(evidence.getSignalType())
                .mimeType(evidence.getMimeType())
                .imageBase64(Base64.getEncoder().encodeToString(evidence.getImageData()))
                .status(evidence.getStatus())
                .clinicianName(evidence.getClinicianName())
                .verificationReason(evidence.getVerificationReason())
                .uploadedAt(evidence.getUploadedAt())
                .verifiedAt(evidence.getVerifiedAt())
                .build();
    }

    public record EvidenceRollup(DailyVerificationState state, int pendingCount, int deniedCount) {}
    public record UserEvidenceSummary(int pendingCount, int approvedCount, int deniedCount) {}
}

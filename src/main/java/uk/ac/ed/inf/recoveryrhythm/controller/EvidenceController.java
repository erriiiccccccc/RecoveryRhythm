package uk.ac.ed.inf.recoveryrhythm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.recoveryrhythm.dto.EvidenceVerificationRequest;
import uk.ac.ed.inf.recoveryrhythm.dto.SignalEvidenceResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.VerificationStatus;
import uk.ac.ed.inf.recoveryrhythm.service.EvidenceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @GetMapping("/pending")
    public ResponseEntity<List<SignalEvidenceResponse>> pendingQueue() {
        return ResponseEntity.ok(evidenceService.getPendingEvidenceQueue());
    }

    @GetMapping("/signal/{signalLogId}")
    public ResponseEntity<List<SignalEvidenceResponse>> signalEvidence(@PathVariable UUID signalLogId) {
        return ResponseEntity.ok(evidenceService.getEvidenceForSignal(signalLogId));
    }

    @PostMapping("/{evidenceId}/approve")
    public ResponseEntity<SignalEvidenceResponse> approve(
            @PathVariable UUID evidenceId,
            @RequestBody(required = false) EvidenceVerificationRequest req
    ) {
        String clinicianName = req != null ? req.getClinicianName() : null;
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(evidenceService.verifyEvidence(evidenceId, VerificationStatus.APPROVED, clinicianName, reason));
    }

    @PostMapping("/{evidenceId}/deny")
    public ResponseEntity<SignalEvidenceResponse> deny(
            @PathVariable UUID evidenceId,
            @RequestBody(required = false) EvidenceVerificationRequest req
    ) {
        String clinicianName = req != null ? req.getClinicianName() : null;
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(evidenceService.verifyEvidence(evidenceId, VerificationStatus.DENIED, clinicianName, reason));
    }
}

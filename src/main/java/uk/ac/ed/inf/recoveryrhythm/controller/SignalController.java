package uk.ac.ed.inf.recoveryrhythm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.recoveryrhythm.dto.DailySignalRequest;
import uk.ac.ed.inf.recoveryrhythm.dto.DailySignalResponse;
import uk.ac.ed.inf.recoveryrhythm.dto.SignalEvidenceResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.EvidenceSignalType;
import uk.ac.ed.inf.recoveryrhythm.service.EvidenceService;
import uk.ac.ed.inf.recoveryrhythm.service.SignalService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/signals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SignalController {

    private final SignalService signalService;
    private final EvidenceService evidenceService;
    private final ObjectMapper objectMapper;

    @PostMapping("/daily")
    public ResponseEntity<DailySignalResponse> logSignal(
            @PathVariable UUID userId,
            @RequestBody DailySignalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signalService.logDailySignal(userId, req));
    }

    @PostMapping(value = "/daily-with-evidence", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> logSignalWithEvidence(
            @PathVariable UUID userId,
            @RequestPart("payload") String payload,
            @RequestPart(value = "medicationEvidence", required = false) MultipartFile medicationEvidence,
            @RequestPart(value = "mealEvidence", required = false) MultipartFile mealEvidence,
            @RequestPart(value = "activityEvidence", required = false) MultipartFile activityEvidence
    ) throws Exception {
        DailySignalRequest req = objectMapper.readValue(payload, DailySignalRequest.class);
        DailySignalResponse signalResponse = signalService.logDailySignal(userId, req);

        Map<EvidenceSignalType, MultipartFile> files = new HashMap<>();
        files.put(EvidenceSignalType.MEDICATION, medicationEvidence);
        files.put(EvidenceSignalType.MEAL, mealEvidence);
        files.put(EvidenceSignalType.ACTIVITY, activityEvidence);
        List<SignalEvidenceResponse> evidence = evidenceService.attachEvidenceToSignal(userId, signalResponse.getId(), files);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "signal", signalResponse,
                "evidence", evidence
        ));
    }

    @GetMapping
    public ResponseEntity<List<DailySignalResponse>> getSignals(@PathVariable UUID userId) {
        return ResponseEntity.ok(signalService.getSignals(userId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DailySignalResponse>> getRecentSignals(@PathVariable UUID userId) {
        return ResponseEntity.ok(signalService.getRecentSignals(userId));
    }
}

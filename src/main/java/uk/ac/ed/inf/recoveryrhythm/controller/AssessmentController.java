package uk.ac.ed.inf.recoveryrhythm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.recoveryrhythm.dto.BaselineSnapshotResponse;
import uk.ac.ed.inf.recoveryrhythm.dto.RiskAssessmentResponse;
import uk.ac.ed.inf.recoveryrhythm.service.BaselineService;
import uk.ac.ed.inf.recoveryrhythm.service.RiskService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final BaselineService baselineService;
    private final RiskService riskService;

    @PostMapping("/baseline/recalculate")
    public ResponseEntity<BaselineSnapshotResponse> recalculateBaseline(@PathVariable UUID userId) {
        return ResponseEntity.ok(baselineService.recalculate(userId));
    }

    @GetMapping("/baseline/latest")
    public ResponseEntity<BaselineSnapshotResponse> getLatestBaseline(@PathVariable UUID userId) {
        return ResponseEntity.ok(baselineService.getLatestBaseline(userId));
    }

    @PostMapping("/risk/recalculate")
    public ResponseEntity<RiskAssessmentResponse> recalculateRisk(@PathVariable UUID userId) {
        return ResponseEntity.ok(riskService.recalculate(userId));
    }

    @GetMapping("/risk/latest")
    public ResponseEntity<RiskAssessmentResponse> getLatestRisk(@PathVariable UUID userId) {
        return ResponseEntity.ok(riskService.getLatest(userId));
    }

    @GetMapping("/risk/history")
    public ResponseEntity<List<RiskAssessmentResponse>> getRiskHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(riskService.getHistory(userId));
    }
}

package uk.ac.ed.inf.recoveryrhythm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.recoveryrhythm.dto.EscalationResponse;
import uk.ac.ed.inf.recoveryrhythm.dto.InterventionResponse;
import uk.ac.ed.inf.recoveryrhythm.dto.RecoveryEpisodeResponse;
import uk.ac.ed.inf.recoveryrhythm.service.EpisodeService;
import uk.ac.ed.inf.recoveryrhythm.service.EscalationService;
import uk.ac.ed.inf.recoveryrhythm.service.InterventionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EpisodeController {

    private final EpisodeService episodeService;
    private final InterventionService interventionService;
    private final EscalationService escalationService;

    @GetMapping("/episodes")
    public ResponseEntity<List<RecoveryEpisodeResponse>> getEpisodes(@PathVariable UUID userId) {
        return ResponseEntity.ok(episodeService.getEpisodes(userId));
    }

    @GetMapping("/episodes/active")
    public ResponseEntity<Optional<RecoveryEpisodeResponse>> getActiveEpisode(@PathVariable UUID userId) {
        return ResponseEntity.ok(episodeService.getActiveEpisodeResponse(userId));
    }

    @GetMapping("/interventions")
    public ResponseEntity<List<InterventionResponse>> getInterventions(@PathVariable UUID userId) {
        return ResponseEntity.ok(interventionService.getInterventions(userId));
    }

    @GetMapping("/escalations")
    public ResponseEntity<List<EscalationResponse>> getEscalations(@PathVariable UUID userId) {
        return ResponseEntity.ok(escalationService.getEscalations(userId));
    }
}

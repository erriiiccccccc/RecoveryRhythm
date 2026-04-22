package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.dto.RecoveryEpisodeResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryEpisode;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.RecoveryEpisodeRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodeService {

    private final RecoveryEpisodeRepository episodeRepo;
    private final UserService userService;
    private final KafkaEventPublisher kafkaPublisher;

    @Transactional
    public Optional<RecoveryEpisode> handleStateTransition(
            RecoveryUser user,
            RecoveryState previousState,
            RecoveryState newState,
            int riskScore,
            String reason) {

        Optional<RecoveryEpisode> activeEpisode = episodeRepo.findByUserAndActiveTrue(user);

        boolean shouldOpen = (newState == RecoveryState.CONCERNING || newState == RecoveryState.ACUTE_RISK)
                && activeEpisode.isEmpty();

        boolean shouldClose = activeEpisode.isPresent()
                && (newState == RecoveryState.STABLE || newState == RecoveryState.RECOVERING
                    || newState == RecoveryState.DRIFTING)
                && (previousState == RecoveryState.CONCERNING || previousState == RecoveryState.ACUTE_RISK);

        if (shouldOpen) {
            RecoveryEpisode episode = RecoveryEpisode.builder()
                    .user(user)
                    .openedAt(LocalDateTime.now())
                    .peakRiskScore(riskScore)
                    .openingReason(reason)
                    .stateAtOpen(newState)
                    .active(true)
                    .build();
            episode = episodeRepo.save(episode);
            kafkaPublisher.publishEpisodeOpened(user.getId(), episode.getId(), reason);
            log.info("[Episode] Opened for user {} (score={}, state={})", user.getDisplayName(), riskScore, newState);
            return Optional.of(episode);
        }

        if (shouldClose && activeEpisode.isPresent()) {
            RecoveryEpisode episode = activeEpisode.get();
            episode.setActive(false);
            episode.setClosedAt(LocalDateTime.now());
            episode.setStateAtClose(newState);
            episode.setClosingReason("Recovery pattern improved. State returned to " + newState.name() + ".");
            episodeRepo.save(episode);
            kafkaPublisher.publishEpisodeClosed(user.getId(), episode.getId(), episode.getClosingReason());
            log.info("[Episode] Closed for user {} (duration={}h)", user.getDisplayName(),
                    Duration.between(episode.getOpenedAt(), episode.getClosedAt()).toHours());
            return Optional.of(episode);
        }

        if (activeEpisode.isPresent() && riskScore > activeEpisode.get().getPeakRiskScore()) {
            RecoveryEpisode episode = activeEpisode.get();
            episode.setPeakRiskScore(riskScore);
            episodeRepo.save(episode);
        }

        return activeEpisode;
    }

    public Optional<RecoveryEpisode> getActiveEpisode(RecoveryUser user) {
        return episodeRepo.findByUserAndActiveTrue(user);
    }

    public List<RecoveryEpisodeResponse> getEpisodes(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return episodeRepo.findByUserOrderByOpenedAtDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public Optional<RecoveryEpisodeResponse> getActiveEpisodeResponse(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return episodeRepo.findByUserAndActiveTrue(user).map(this::toResponse);
    }

    public RecoveryEpisodeResponse toResponse(RecoveryEpisode e) {
        LocalDateTime end = e.getClosedAt() != null ? e.getClosedAt() : LocalDateTime.now();
        long hours = Duration.between(e.getOpenedAt(), end).toHours();
        return RecoveryEpisodeResponse.builder()
                .id(e.getId())
                .userId(e.getUser().getId())
                .openedAt(e.getOpenedAt())
                .closedAt(e.getClosedAt())
                .peakRiskScore(e.getPeakRiskScore())
                .openingReason(e.getOpeningReason())
                .closingReason(e.getClosingReason())
                .stateAtOpen(e.getStateAtOpen())
                .stateAtClose(e.getStateAtClose())
                .active(e.isActive())
                .durationHours(hours)
                .build();
    }
}

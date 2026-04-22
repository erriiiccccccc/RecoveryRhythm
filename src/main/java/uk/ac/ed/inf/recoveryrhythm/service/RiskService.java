package uk.ac.ed.inf.recoveryrhythm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.dto.ContributingFactor;
import uk.ac.ed.inf.recoveryrhythm.dto.RiskAssessmentResponse;
import uk.ac.ed.inf.recoveryrhythm.engine.RiskEngine;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;
import uk.ac.ed.inf.recoveryrhythm.entity.RiskAssessment;
import uk.ac.ed.inf.recoveryrhythm.repository.RecoveryUserRepository;
import uk.ac.ed.inf.recoveryrhythm.repository.RiskAssessmentRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskService {

    private final RiskEngine riskEngine;
    private final RiskAssessmentRepository assessmentRepo;
    private final RecoveryUserRepository userRepo;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RiskAssessmentResponse recalculate(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        RiskAssessmentResponse result = riskEngine.assess(user);
        userRepo.save(user);
        return result;
    }

    public RiskAssessmentResponse getLatest(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        RiskAssessment assessment = assessmentRepo.findTopByUserOrderByAssessedAtDesc(user)
                .orElseThrow(() -> new IllegalStateException("No risk assessment found for user " + userId));
        return toResponse(assessment);
    }

    public List<RiskAssessmentResponse> getHistory(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return assessmentRepo.findByUserOrderByAssessedAtDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public RiskAssessmentResponse toResponse(RiskAssessment a) {
        List<ContributingFactor> factors = List.of();
        try {
            if (a.getFactorBreakdownJson() != null) {
                factors = objectMapper.readValue(a.getFactorBreakdownJson(),
                        new TypeReference<List<ContributingFactor>>() {});
            }
        } catch (Exception e) {
            log.warn("Could not deserialize factor breakdown: {}", e.getMessage());
        }

        return RiskAssessmentResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .assessedAt(a.getAssessedAt())
                .riskScore(a.getRiskScore())
                .state(a.getState())
                .conciseSummary(a.getConciseSummary())
                .detailedExplanation(a.getDetailedExplanation())
                .contributingFactors(factors)
                .build();
    }
}

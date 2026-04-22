package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.dto.*;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;
import uk.ac.ed.inf.recoveryrhythm.entity.SupportContact;
import uk.ac.ed.inf.recoveryrhythm.repository.RecoveryUserRepository;
import uk.ac.ed.inf.recoveryrhythm.repository.SupportContactRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final RecoveryUserRepository userRepo;
    private final SupportContactRepository supportContactRepo;

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        RecoveryUser user = RecoveryUser.builder()
                .displayName(req.getDisplayName())
                .recoveryStartDate(req.getRecoveryStartDate())
                .build();
        user = userRepo.save(user);
        log.info("Created user: {} ({})", user.getDisplayName(), user.getId());
        return toUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll().stream().map(this::toUserResponse).toList();
    }

    public UserResponse getUser(UUID id) {
        return toUserResponse(requireUser(id));
    }

    public RecoveryUser requireUser(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public SupportContactResponse addSupportContact(UUID userId, CreateSupportContactRequest req) {
        RecoveryUser user = requireUser(userId);
        SupportContact contact = SupportContact.builder()
                .user(user)
                .name(req.getName())
                .relationship(req.getRelationship())
                .contactChannel(req.getContactChannel())
                .contactValue(req.getContactValue())
                .escalationEnabled(req.isEscalationEnabled())
                .build();
        contact = supportContactRepo.save(contact);
        log.info("Added support contact {} for user {}", contact.getName(), user.getDisplayName());
        return toContactResponse(contact);
    }

    public List<SupportContactResponse> getSupportContacts(UUID userId) {
        RecoveryUser user = requireUser(userId);
        return supportContactRepo.findByUser(user).stream()
                .map(this::toContactResponse)
                .toList();
    }

    public UserResponse toUserResponse(RecoveryUser u) {
        return UserResponse.builder()
                .id(u.getId())
                .displayName(u.getDisplayName())
                .recoveryStartDate(u.getRecoveryStartDate())
                .currentState(u.getCurrentState())
                .currentRiskScore(u.getCurrentRiskScore())
                .reentryModeActive(u.isReentryModeActive())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    private SupportContactResponse toContactResponse(SupportContact c) {
        return SupportContactResponse.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .name(c.getName())
                .relationship(c.getRelationship())
                .contactChannel(c.getContactChannel())
                .contactValue(c.getContactValue())
                .escalationEnabled(c.isEscalationEnabled())
                .createdAt(c.getCreatedAt())
                .build();
    }
}

package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
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
    private final S3StorageService s3StorageService;

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        RecoveryUser user = createAndPersistUser(req);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse createUserWithProfile(CreateUserRequest req, MultipartFile profilePhoto) {
        RecoveryUser user = createAndPersistUser(req);
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            try {
                String key = s3StorageService.putProfilePhoto(
                        user.getId(),
                        profilePhoto.getOriginalFilename(),
                        profilePhoto.getContentType(),
                        profilePhoto.getBytes()
                );
                user.setProfilePhotoObjectKey(key);
                user.setProfilePhotoMimeType(profilePhoto.getContentType());
                user = userRepo.save(user);
            } catch (Exception ex) {
                throw new IllegalStateException("Could not upload profile photo", ex);
            }
        }
        return toUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll().stream().map(this::toUserResponse).toList();
    }

    public List<PatientLoginAccountResponse> getPatientLoginAccounts() {
        return userRepo.findByLoginEmailIsNotNullOrderByDisplayNameAsc().stream()
                .filter(u -> u.getLoginPassword() != null && !u.getLoginPassword().isBlank())
                .map(u -> PatientLoginAccountResponse.builder()
                        .userId(u.getId())
                        .displayName(u.getDisplayName())
                        .email(u.getLoginEmail())
                        .password(u.getLoginPassword())
                        .build())
                .toList();
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
        String profileBase64 = null;
        if (u.getProfilePhotoObjectKey() != null) {
            try {
                profileBase64 = java.util.Base64.getEncoder().encodeToString(
                        s3StorageService.getEvidenceObject(u.getProfilePhotoObjectKey())
                );
            } catch (Exception ex) {
                log.warn("Could not load profile photo for user {}: {}", u.getId(), ex.getMessage());
            }
        }
        return UserResponse.builder()
                .id(u.getId())
                .displayName(u.getDisplayName())
                .loginEmail(u.getLoginEmail())
                .recoveryStartDate(u.getRecoveryStartDate())
                .currentState(u.getCurrentState())
                .currentRiskScore(u.getCurrentRiskScore())
                .reentryModeActive(u.isReentryModeActive())
                .baselineIntakeNotes(u.getBaselineIntakeNotes())
                .typicalSleepStartHour(u.getTypicalSleepStartHour())
                .expectedActivityDaysPerWeek(u.getExpectedActivityDaysPerWeek())
                .expectedMedicationDosesPerDay(u.getExpectedMedicationDosesPerDay())
                .expectedMedicationSchedule(u.getExpectedMedicationSchedule())
                .expectedMealsPerDay(u.getExpectedMealsPerDay())
                .expectedActivityType(u.getExpectedActivityType())
                .expectedSleepTarget(u.getExpectedSleepTarget())
                .baselineReferenceSource(u.getBaselineReferenceSource())
                .profilePhotoMimeType(u.getProfilePhotoMimeType())
                .profilePhotoBase64(profileBase64)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    private RecoveryUser createAndPersistUser(CreateUserRequest req) {
        if (req.getLoginEmail() != null && !req.getLoginEmail().isBlank()) {
            userRepo.findByLoginEmailIgnoreCase(req.getLoginEmail().trim())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Login email already exists: " + req.getLoginEmail());
                    });
        }
        RecoveryUser user = RecoveryUser.builder()
                .displayName(req.getDisplayName())
                .loginEmail(req.getLoginEmail() == null ? null : req.getLoginEmail().trim().toLowerCase())
                .loginPassword(req.getLoginPassword())
                .recoveryStartDate(req.getRecoveryStartDate())
                .baselineIntakeNotes(req.getBaselineIntakeNotes())
                .typicalSleepStartHour(req.getTypicalSleepStartHour())
                .expectedActivityDaysPerWeek(req.getExpectedActivityDaysPerWeek())
                .expectedMedicationDosesPerDay(req.getExpectedMedicationDosesPerDay())
                .expectedMedicationSchedule(req.getExpectedMedicationSchedule())
                .expectedMealsPerDay(req.getExpectedMealsPerDay())
                .expectedActivityType(req.getExpectedActivityType())
                .expectedSleepTarget(req.getExpectedSleepTarget())
                .baselineReferenceSource(req.getBaselineReferenceSource())
                .build();
        user = userRepo.save(user);
        log.info("Created user: {} ({})", user.getDisplayName(), user.getId());
        return user;
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

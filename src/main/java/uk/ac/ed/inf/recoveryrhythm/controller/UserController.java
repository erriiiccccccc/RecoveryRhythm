package uk.ac.ed.inf.recoveryrhythm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.recoveryrhythm.dto.*;
import uk.ac.ed.inf.recoveryrhythm.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping("/{id}/support-contacts")
    public ResponseEntity<SupportContactResponse> addContact(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSupportContactRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addSupportContact(id, req));
    }

    @GetMapping("/{id}/support-contacts")
    public ResponseEntity<List<SupportContactResponse>> getContacts(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getSupportContacts(id));
    }
}

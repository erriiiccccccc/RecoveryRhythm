package uk.ac.ed.inf.recoveryrhythm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {

    @NotBlank
    private String displayName;

    @NotNull
    private LocalDate recoveryStartDate;
}

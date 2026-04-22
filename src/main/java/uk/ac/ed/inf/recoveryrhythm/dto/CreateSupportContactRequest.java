package uk.ac.ed.inf.recoveryrhythm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSupportContactRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String relationship;

    @NotBlank
    private String contactChannel;

    @NotBlank
    private String contactValue;

    private boolean escalationEnabled = true;
}

package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributingFactor {
    private String factor;
    private int impact;
    private String details;
    private String severity;
}

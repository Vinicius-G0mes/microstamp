package microstamp.step2.dto.component;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import microstamp.step2.enumeration.ComponentType;
import microstamp.step2.enumeration.Style;

import java.util.UUID;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacadeComponentInsertDto {

    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    private Boolean isVisible;

    private String fatherCode;

    private UUID fatherId;

    private Style border;

    private ComponentType type;
}

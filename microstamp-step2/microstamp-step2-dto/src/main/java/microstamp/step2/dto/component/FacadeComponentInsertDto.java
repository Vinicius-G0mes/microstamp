package microstamp.step2.dto.component;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import microstamp.step2.enumeration.ComponentType;
import microstamp.step2.enumeration.Style;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacadeComponentInsertDto {


    @NotBlank
    private String name;

    @NotBlank
    private String code;

    private Boolean isVisible;

    private String fatherCode;

    private Style border;

    private ComponentType type;
}

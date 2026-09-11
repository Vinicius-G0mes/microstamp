package microstamp.step2.dto.connection;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import microstamp.step2.dto.interaction.FacadeInteractionInsertDto;
import microstamp.step2.enumeration.Style;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionBatchInsertDto {

    private UUID id;

    @NotBlank
    private String code;

    private String sourceCode;

    private String targetCode;

    private Style style;

    private List<FacadeInteractionInsertDto> interactions;
}

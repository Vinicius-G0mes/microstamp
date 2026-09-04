package microstamp.step2.dto.connection;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import microstamp.step2.dto.interaction.InteractionInsertDto;
import microstamp.step2.enumeration.Style;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionBatchInsertDto {

    @NotBlank
    private String code;

    private String sourceCode;

    private String targetCode;

    private Style style;

    private List<InteractionInsertDto> interactions;
}

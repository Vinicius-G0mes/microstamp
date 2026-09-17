package microstamp.step2.dto.controlstructure;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import microstamp.step2.dto.component.FacadeComponentInsertDto;
import microstamp.step2.dto.connection.FacadeConnectionInsertDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlStructureInsertDto {

    @NotNull
    private UUID analysisId;

    private List<FacadeComponentInsertDto> components = new ArrayList<>();
    private List<FacadeConnectionInsertDto> connections = new ArrayList<>();
}

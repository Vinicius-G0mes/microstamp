package microstamp.step2.dto.controlstructure;

import lombok.*;
import microstamp.step2.dto.component.ComponentInsertDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlStructureInsertDto {

    private UUID analysisId;

    private List<ComponentInsertDto> components = new ArrayList<>();

    //adicionar posteriormente a lista de connections

}

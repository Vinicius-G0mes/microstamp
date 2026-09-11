package microstamp.step2.dto.interaction;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacadeInteractionInsertDto extends InteractionInsertDto{

    private UUID id;
}
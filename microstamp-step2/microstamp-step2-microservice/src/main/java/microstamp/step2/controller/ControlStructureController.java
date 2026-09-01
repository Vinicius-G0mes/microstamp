package microstamp.step2.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import microstamp.step2.dto.controlstructure.ControlStructureInsertDto;
import microstamp.step2.service.ControlStructureService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/control-structures")
public class ControlStructureController {

    private final ControlStructureService controlStructureService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody ControlStructureInsertDto dto){
        controlStructureService.createControlStructure(dto);
    }

}

package microstamp.step2.service;


import microstamp.step2.dto.controlstructure.ControlStructureInsertDto;

public interface ControlStructureService {
    void createControlStructure(ControlStructureInsertDto dto);
    void updateControlStructure(ControlStructureInsertDto dto);
}

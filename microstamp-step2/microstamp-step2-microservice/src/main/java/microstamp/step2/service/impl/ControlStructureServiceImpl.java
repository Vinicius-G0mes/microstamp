package microstamp.step2.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import microstamp.step2.dto.component.ComponentInsertDto;
import microstamp.step2.dto.controlstructure.ControlStructureInsertDto;
import microstamp.step2.service.ComponentService;
import microstamp.step2.service.ControlStructureService;

import java.util.List;
import java.util.UUID;

@Component
public class ControlStructureServiceImpl implements ControlStructureService {

    @Autowired
    private ComponentService componentService;

    @Override
    @Transactional
    public void createControlStructure(ControlStructureInsertDto dto) {
        if (dto.getComponents() != null && !dto.getComponents().isEmpty()) {
            saveComponents(dto.getComponents(), dto.getAnalysisId());
        }
    }

    private void saveComponents(List<ComponentInsertDto> components, UUID analysisId) {
        for (ComponentInsertDto componentDto : components) {
            componentDto.setAnalysisId(analysisId);
            componentService.insert(componentDto);
        }
    }
}
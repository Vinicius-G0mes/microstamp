package microstamp.step2.service.impl;

import microstamp.step2.dto.component.ComponentReadDto;
import microstamp.step2.dto.connection.ConnectionBatchInsertDto;
import microstamp.step2.dto.connection.ConnectionInsertDto;
import microstamp.step2.service.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import microstamp.step2.dto.component.ComponentInsertDto;
import microstamp.step2.dto.controlstructure.ControlStructureInsertDto;
import microstamp.step2.service.ComponentService;
import microstamp.step2.service.ControlStructureService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ControlStructureServiceImpl implements ControlStructureService {

    @Autowired
    private ComponentService componentService;

    @Autowired
    private ConnectionService connectionService;

    @Override
    @Transactional
    public void createControlStructure(ControlStructureInsertDto dto) {
        Map<String, UUID> componentCodeToIdMap = new HashMap<>();

        if (dto.getComponents() != null && !dto.getComponents().isEmpty()) {
            componentCodeToIdMap = saveComponents(dto.getComponents(), dto.getAnalysisId());
        }

        if (dto.getConnections() != null && !dto.getConnections().isEmpty()) {
            saveConnections(dto.getConnections(), dto.getAnalysisId(), componentCodeToIdMap);
        }
    }

    private Map<String, UUID> saveComponents(List<ComponentInsertDto> components, UUID analysisId) {
        Map<String, UUID> map = new HashMap<>();

        for (ComponentInsertDto componentDto : components) {
            componentDto.setAnalysisId(analysisId);
            ComponentReadDto savedComponent = componentService.insert(componentDto);
            map.put(savedComponent.getCode(), savedComponent.getId());
        }

        return map;
    }

    private void saveConnections(List<ConnectionBatchInsertDto> connections, UUID analysisId, Map<String, UUID> componentMap) {
        for (ConnectionBatchInsertDto connBatchDto : connections) {
            UUID sourceId = componentMap.get(connBatchDto.getSourceCode());
            UUID targetId = componentMap.get(connBatchDto.getTargetCode());

            if (sourceId != null && targetId != null) {
                ConnectionInsertDto connectionInsertDto = ConnectionInsertDto.builder()
                        .code(connBatchDto.getCode())
                        .style(connBatchDto.getStyle())
                        .sourceId(sourceId)
                        .targetId(targetId)
                        .analysisId(analysisId)
                        .build();

                connectionService.insert(connectionInsertDto);
            }
        }
    }
}
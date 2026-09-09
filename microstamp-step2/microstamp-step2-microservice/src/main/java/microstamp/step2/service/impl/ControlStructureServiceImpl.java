package microstamp.step2.service.impl;

import microstamp.step2.client.MicroStampAuthClient;
import microstamp.step2.dto.component.ComponentReadDto;
import microstamp.step2.dto.component.ComponentUpdateDto;
import microstamp.step2.dto.component.FacadeComponentInsertDto;
import microstamp.step2.dto.connection.ConnectionBatchInsertDto;
import microstamp.step2.dto.connection.ConnectionInsertDto;
import microstamp.step2.dto.connection.ConnectionReadDto;
import microstamp.step2.dto.interaction.InteractionInsertDto;
import microstamp.step2.service.ConnectionService;
import microstamp.step2.service.InteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import microstamp.step2.dto.component.ComponentInsertDto;
import microstamp.step2.dto.controlstructure.ControlStructureInsertDto;
import microstamp.step2.service.ComponentService;
import microstamp.step2.service.ControlStructureService;

import java.util.*;

@Component
public class ControlStructureServiceImpl implements ControlStructureService {

    @Autowired
    private ComponentService componentService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private MicroStampAuthClient microStampAuthClient;

    @Override
    @Transactional
    public void createControlStructure(ControlStructureInsertDto dto) {

        microStampAuthClient.getAnalysisById(dto.getAnalysisId());
        Map<String, UUID> componentCodeToIdMap = new HashMap<>();

        if (dto.getComponents() != null && !dto.getComponents().isEmpty()) {
            componentCodeToIdMap = saveComponents(dto.getComponents(), dto.getAnalysisId());
        }

        if (dto.getConnections() != null && !dto.getConnections().isEmpty()) {
            saveConnections(dto.getConnections(), dto.getAnalysisId(), componentCodeToIdMap);
        }
    }

    private Map<String, UUID> saveComponents(List<FacadeComponentInsertDto> components, UUID analysisId) {
        Map<String, UUID> componentCodeToIdMap = new HashMap<>();
        List<FacadeComponentInsertDto> componentsContainingFather = new ArrayList<>();

        for (FacadeComponentInsertDto facadeComponentDto : components) {
            ComponentInsertDto insertDto = new ComponentInsertDto();
            insertDto.setName(facadeComponentDto.getName());
            insertDto.setCode(facadeComponentDto.getCode());
            insertDto.setIsVisible(facadeComponentDto.getIsVisible());
            insertDto.setType(facadeComponentDto.getType());
            insertDto.setBorder(facadeComponentDto.getBorder());
            insertDto.setAnalysisId(analysisId);
            insertDto.setFatherId(null);

            if(facadeComponentDto.getFatherCode() != null){
                componentsContainingFather.add(facadeComponentDto);
            }

            ComponentReadDto savedComponent = componentService.insert(insertDto);
            componentCodeToIdMap.put(savedComponent.getCode(), savedComponent.getId());
        }

        for (FacadeComponentInsertDto facadeComponentDto : componentsContainingFather) {
            if (facadeComponentDto.getFatherCode() != null && !facadeComponentDto.getFatherCode().isBlank()) {
                UUID childId = componentCodeToIdMap.get(facadeComponentDto.getCode());
                UUID fatherId = componentCodeToIdMap.get(facadeComponentDto.getFatherCode());

                if (childId != null && fatherId != null) {
                    ComponentUpdateDto updateDto = ComponentUpdateDto.builder()
                            .name(facadeComponentDto.getName())
                            .code(facadeComponentDto.getCode())
                            .isVisible(facadeComponentDto.getIsVisible())
                            .type(facadeComponentDto.getType())
                            .border(facadeComponentDto.getBorder())
                            .fatherId(fatherId)
                            .build();

                    componentService.update(childId, updateDto);
                }
            }
        }

        return componentCodeToIdMap;
    }

    private void saveConnections(List<ConnectionBatchInsertDto> connections, UUID analysisId, Map<String, UUID> componentMap) {
        for (ConnectionBatchInsertDto connectionBatchInsertDto : connections) {
            UUID sourceId = componentMap.get(connectionBatchInsertDto.getSourceCode());
            UUID targetId = componentMap.get(connectionBatchInsertDto.getTargetCode());

            if (sourceId != null && targetId != null) {
                ConnectionInsertDto connectionInsertDto = ConnectionInsertDto.builder()
                        .code(connectionBatchInsertDto.getCode())
                        .style(connectionBatchInsertDto.getStyle())
                        .sourceId(sourceId)
                        .targetId(targetId)
                        .analysisId(analysisId)
                        .build();

                ConnectionReadDto savedConnection = connectionService.insert(connectionInsertDto);

                if (connectionBatchInsertDto.getInteractions() != null && !connectionBatchInsertDto.getInteractions().isEmpty()) {
                    saveInteractions(connectionBatchInsertDto, savedConnection);
                }
            }
        }
    }

    private void saveInteractions(ConnectionBatchInsertDto connectionBatchInsertDto, ConnectionReadDto savedConnection){
        for (InteractionInsertDto interactionDto : connectionBatchInsertDto.getInteractions()) {
            interactionDto.setConnectionId(savedConnection.getId());
            interactionService.insert(interactionDto);
        }
    }
}
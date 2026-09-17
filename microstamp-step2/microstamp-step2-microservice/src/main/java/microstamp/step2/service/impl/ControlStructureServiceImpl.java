package microstamp.step2.service.impl;

import microstamp.step2.client.MicroStampAuthClient;
import microstamp.step2.dto.component.ComponentReadDto;
import microstamp.step2.dto.component.ComponentUpdateDto;
import microstamp.step2.dto.component.FacadeComponentInsertDto;
import microstamp.step2.dto.connection.FacadeConnectionInsertDto;
import microstamp.step2.dto.connection.ConnectionInsertDto;
import microstamp.step2.dto.connection.ConnectionReadDto;
import microstamp.step2.dto.interaction.FacadeInteractionInsertDto;
import microstamp.step2.dto.interaction.InteractionReadDto;
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
            insertDto.setFatherId(facadeComponentDto.getFatherId());

            if (facadeComponentDto.getFatherCode() != null) {
                componentsContainingFather.add(facadeComponentDto);
            }

            ComponentReadDto savedComponent = componentService.insert(insertDto);
            componentCodeToIdMap.put(savedComponent.getCode(), savedComponent.getId());
        }

        for (FacadeComponentInsertDto facadeComponentDto : componentsContainingFather) {
            if (!facadeComponentDto.getFatherCode().isBlank() && facadeComponentDto.getFatherId() == null) {
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

    private void saveConnections(List<FacadeConnectionInsertDto> connections, UUID analysisId, Map<String, UUID> componentMap) {
        for (FacadeConnectionInsertDto facadeConnectionInsertDto : connections) {
            UUID sourceId = componentMap.get(facadeConnectionInsertDto.getSourceCode());
            UUID targetId = componentMap.get(facadeConnectionInsertDto.getTargetCode());

            if (sourceId != null && targetId != null) {
                ConnectionInsertDto connectionInsertDto = ConnectionInsertDto.builder()
                        .code(facadeConnectionInsertDto.getCode())
                        .style(facadeConnectionInsertDto.getStyle())
                        .sourceId(sourceId)
                        .targetId(targetId)
                        .analysisId(analysisId)
                        .build();

                ConnectionReadDto savedConnection = connectionService.insert(connectionInsertDto);

                if (facadeConnectionInsertDto.getInteractions() != null && !facadeConnectionInsertDto.getInteractions().isEmpty()) {
                    saveInteractions(facadeConnectionInsertDto, savedConnection);
                }
            }
        }
    }

    private void saveInteractions(FacadeConnectionInsertDto facadeConnectionInsertDto, ConnectionReadDto savedConnection) {
        for (FacadeInteractionInsertDto interactionDto : facadeConnectionInsertDto.getInteractions()) {
            interactionDto.setConnectionId(savedConnection.getId());
            interactionService.insert(interactionDto);
        }
    }

    @Override
    @Transactional
    public void updateControlStructure(ControlStructureInsertDto dto) {
        microStampAuthClient.getAnalysisById(dto.getAnalysisId());
        UUID analysisId = dto.getAnalysisId();

        List<FacadeComponentInsertDto> incomingNewComponents = new LinkedList<>();
        List<FacadeComponentInsertDto> incomingExistingComponents = new LinkedList<>();
        Set<UUID> incomingPersistedComponentsIds = new HashSet<>();
        storeIncomingComponents(dto, incomingPersistedComponentsIds, incomingNewComponents, incomingExistingComponents);

        Set<UUID> incomingPersistedConnectionsIds = new HashSet<>();
        storeIncomingConnections(dto, incomingPersistedConnectionsIds);

        Set<UUID> incomingPersistedInteractionsIds = new HashSet<>();
        storeIncomingInteractions(dto, incomingPersistedInteractionsIds);

        List<InteractionReadDto> dbInteractions = interactionService.findByAnalysisId(analysisId);
        cleanUpRemovedInteractions(dbInteractions, incomingPersistedInteractionsIds);

        List<ConnectionReadDto> dbConnections = connectionService.findByAnalysisId(analysisId);
        cleanUpRemovedConnections(dbConnections, incomingPersistedConnectionsIds);

        List<ComponentReadDto> dbComponents = componentService.findByAnalysisId(analysisId);
        cleanUpRemovedComponents(dbComponents, incomingPersistedComponentsIds);

        Map<String, UUID> componentCodeToIdMap = new HashMap<>();
        if (!incomingNewComponents.isEmpty()) {
            componentCodeToIdMap = saveComponents(incomingNewComponents, analysisId);
        }

        for (FacadeComponentInsertDto existingDto : incomingExistingComponents) {
            componentCodeToIdMap.put(existingDto.getCode(), existingDto.getId());
        }

        updateExistingComponents(incomingExistingComponents, componentCodeToIdMap);
    }

    private void storeIncomingComponents(
            ControlStructureInsertDto dto,
            Set<UUID> incomingPersistedComponents,
            List<FacadeComponentInsertDto> incomingNewComponents,
            List<FacadeComponentInsertDto> incomingExistingComponents) {

        if (dto.getComponents() != null) {
            for (FacadeComponentInsertDto component : dto.getComponents()) {
                UUID id = component.getId();
                if (id != null) {
                    incomingPersistedComponents.add(id);
                    incomingExistingComponents.add(component);
                } else {
                    incomingNewComponents.add(component);
                }
            }
        }
    }

    private void updateExistingComponents(
            List<FacadeComponentInsertDto> incomingExistingComponents,
            Map<String, UUID> componentCodeToIdMap) {

        for (FacadeComponentInsertDto existingDto : incomingExistingComponents) {
            UUID fatherId = existingDto.getFatherId();

            if (fatherId == null && existingDto.getFatherCode() != null && !existingDto.getFatherCode().isBlank()) {
                fatherId = componentCodeToIdMap.get(existingDto.getFatherCode());
            }

            ComponentUpdateDto updateDto = ComponentUpdateDto.builder()
                    .name(existingDto.getName())
                    .code(existingDto.getCode())
                    .isVisible(existingDto.getIsVisible())
                    .type(existingDto.getType())
                    .border(existingDto.getBorder())
                    .fatherId(fatherId)
                    .build();

            componentService.update(existingDto.getId(), updateDto);
        }
    }

    private void storeIncomingConnections(ControlStructureInsertDto dto, Set<UUID> incomingPersistedConnectionsIds) {
        for (FacadeConnectionInsertDto connection : dto.getConnections()) {
            UUID connectionId = connection.getId();

            if (connectionId != null) {
                incomingPersistedConnectionsIds.add(connectionId);
            }
        }
    }

    private void storeIncomingInteractions(ControlStructureInsertDto dto, Set<UUID> incomingPersistedInteractionsIds) {
        for (FacadeConnectionInsertDto connection : dto.getConnections()) {
            if (connection.getInteractions() != null && !connection.getInteractions().isEmpty()) {
                for (FacadeInteractionInsertDto interaction : connection.getInteractions()) {
                    UUID interactionId = interaction.getId();

                    if (interactionId != null) {
                        incomingPersistedInteractionsIds.add(interactionId);
                    }
                }
            }
        }
    }

    private void cleanUpRemovedInteractions(List<InteractionReadDto> dbInteractions, Set<UUID> incomingPersistedInteractionsIds){
        for (InteractionReadDto dbInteraction : dbInteractions) {
            if (!incomingPersistedInteractionsIds.contains(dbInteraction.getId())) {
                interactionService.deleteDirectlyById(dbInteraction.getId());
            }
        }
    }

    private void cleanUpRemovedConnections(List<ConnectionReadDto> dbConnections, Set<UUID> incomingPersistedConnectionsIds){
        for (ConnectionReadDto dbConnection : dbConnections) {
            if (!incomingPersistedConnectionsIds.contains(dbConnection.getId())) {
                connectionService.delete(dbConnection.getId());
            }
        }
    }

    private void cleanUpRemovedComponents(List<ComponentReadDto> dbComponents, Set<UUID> incomingPersistedComponentsIds){
        for (ComponentReadDto dbComponent : dbComponents) {
            if (!incomingPersistedComponentsIds.contains(dbComponent.getId())) {
                componentService.delete(dbComponent.getId());
            }
        }
    }
}
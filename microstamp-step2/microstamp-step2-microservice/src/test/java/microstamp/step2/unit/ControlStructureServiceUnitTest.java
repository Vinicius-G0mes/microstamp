package microstamp.step2.unit;

import microstamp.step2.client.MicroStampAuthClient;
import microstamp.step2.dto.component.ComponentInsertDto;
import microstamp.step2.dto.component.ComponentReadDto;
import microstamp.step2.dto.component.ComponentUpdateDto;
import microstamp.step2.dto.connection.ConnectionInsertDto;
import microstamp.step2.dto.connection.ConnectionReadDto;
import microstamp.step2.dto.connection.ConnectionUpdateDto;
import microstamp.step2.dto.controlstructure.ControlStructureInsertDto;
import microstamp.step2.dto.component.FacadeComponentInsertDto;
import microstamp.step2.dto.connection.FacadeConnectionInsertDto;
import microstamp.step2.dto.interaction.FacadeInteractionInsertDto;
import microstamp.step2.dto.interaction.InteractionReadDto;
import microstamp.step2.exception.Step2NotFoundException;
import microstamp.step2.service.ComponentService;
import microstamp.step2.service.ConnectionService;
import microstamp.step2.service.InteractionService;
import microstamp.step2.service.impl.ControlStructureServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static microstamp.step2.enumeration.ComponentType.*;
import static microstamp.step2.enumeration.InteractionType.*;
import static microstamp.step2.enumeration.Style.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ControlStructureServiceUnitTest {

    @InjectMocks
    private ControlStructureServiceImpl service;

    @Mock
    private ComponentService componentService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private InteractionService interactionService;

    @Mock
    private MicroStampAuthClient microStampAuthClient;

    @Test
    @DisplayName("When analysis is not found > Throw an exception")
    void WhenAnalysisIsNotFoundThrowAnException() {
        UUID analysisId = UUID.randomUUID();
        ControlStructureInsertDto dto = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .build();

        doThrow(new Step2NotFoundException("Analysis not found", analysisId.toString()))
                .when(microStampAuthClient).getAnalysisById(analysisId);

        assertThrows(Step2NotFoundException.class, () -> service.saveControlStructure(dto));
        verify(componentService, never()).findByAnalysisId(any());
    }

    @Test
    @DisplayName("When a new component is added to payload > insert component successfully")
    void WhenNewComponentIsAddedShouldInsertComponentSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID newCompId = UUID.randomUUID();

        // Payload brings a new component
        FacadeComponentInsertDto newComponentInsertDto = FacadeComponentInsertDto.builder()
                .code("C.01")
                .name("Novo Controlador")
                .border(SOLID)
                .isVisible(true)
                .type(CONTROLLER)
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(newComponentInsertDto))
                .connections(List.of())
                .build();

        // Clean Db mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of());

        // Return mock
        ComponentReadDto createdComponentDto = ComponentReadDto.builder()
                .id(newCompId)
                .code("C.01")
                .name("Novo Controlador")
                .border(SOLID)
                .isVisible(true)
                .type("Controller")
                .build();

        when(componentService.insert(any())).thenReturn(createdComponentDto);

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(componentService, times(1)).insert(argThat(dto ->
                dto != null
                        && "C.01".equals(dto.getCode())
                        && "Novo Controlador".equals(dto.getName())
                        && analysisId.equals(dto.getAnalysisId())
        ));
    }

    @Test
    @DisplayName("When a new connection is added to payload > insert connection successfully")
    void WhenNewConnectionIsAddedShouldInsertConnectionSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID newConnectionId = UUID.randomUUID();

        // Persisted db components mocks
        ComponentReadDto sourceDto = ComponentReadDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto targetDto = ComponentReadDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type("Controlled_Process").build();

        FacadeComponentInsertDto sourceInsertDto = FacadeComponentInsertDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto targetInsertDto = FacadeComponentInsertDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        // Payload bringing one connection
        FacadeConnectionInsertDto newConnectionInsertDto = FacadeConnectionInsertDto.builder()
                .code("Cn.01")
                .sourceCode("C.01")
                .targetCode("C.02")
                .style(SOLID)
                .interactions(List.of())
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(sourceInsertDto, targetInsertDto))
                .connections(List.of(newConnectionInsertDto))
                .build();

        // Db mocks (still missing a connection)
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(sourceDto, targetDto));

        ConnectionReadDto createdConnectionDto = ConnectionReadDto.builder()
                .id(newConnectionId)
                .code("Cn.01")
                .source(sourceDto)
                .target(targetDto)
                .style(SOLID)
                .interactions(List.of())
                .build();

        when(connectionService.insert(any())).thenReturn(createdConnectionDto);

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(connectionService, times(1)).insert(argThat(dto ->
                dto != null
                        && "Cn.01".equals(dto.getCode())
                        && sourceId.equals(dto.getSourceId())
                        && targetId.equals(dto.getTargetId())
        ));
    }

    @Test
    @DisplayName("When a new interaction is added to a connection > insert interaction successfully")
    void WhenNewInteractionIsAddedShouldInsertInteractionSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID connId = UUID.randomUUID();

        ComponentReadDto sourceDto = ComponentReadDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto targetDto = ComponentReadDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type("Controlled_Process").build();

        FacadeComponentInsertDto sourceInsertDto = FacadeComponentInsertDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto targetInsertDto = FacadeComponentInsertDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        // New interaction Dto
        FacadeInteractionInsertDto newInteractionInsertDto = new FacadeInteractionInsertDto();
        newInteractionInsertDto.setCode("IN.01");
        newInteractionInsertDto.setName("Ação de Controle A");

        // Payload bringing the connection + interaction
        FacadeConnectionInsertDto connectionInsertDto = FacadeConnectionInsertDto.builder()
                .id(connId)
                .code("Cn.01")
                .sourceCode("C.01")
                .targetCode("C.02")
                .style(SOLID)
                .interactions(List.of(newInteractionInsertDto))
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(sourceInsertDto, targetInsertDto))
                .connections(List.of(connectionInsertDto))
                .build();

        // Persisted connection in Db
        ConnectionReadDto existingConnectionReadDto = ConnectionReadDto.builder()
                .id(connId)
                .code("Cn.01")
                .source(sourceDto)
                .target(targetDto)
                .style(SOLID)
                .interactions(List.of())
                .build();

        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(existingConnectionReadDto));
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(sourceDto, targetDto));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(interactionService, times(1)).insert(argThat(dto ->
                dto != null
                        && "IN.01".equals(dto.getCode())
                        && connId.equals(dto.getConnectionId())
        ));
    }

    @Test
    @DisplayName("When control structure contains new elements > Insert all elements successfully")
    void WhenControlStructureContainsNewElementsInsertAllElementsSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID generatedCompId1 = UUID.randomUUID();
        UUID generatedCompId2 = UUID.randomUUID();
        UUID generatedConnId = UUID.randomUUID();

        // 1. Incoming DTOs without IDs
        FacadeComponentInsertDto newComp1 = new FacadeComponentInsertDto();
        newComp1.setCode("C.01");
        newComp1.setName("Motorista");
        newComp1.setType(CONTROLLER);

        FacadeComponentInsertDto newComp2 = new FacadeComponentInsertDto();
        newComp2.setCode("C.02");
        newComp2.setName("Carro");
        newComp2.setType(CONTROLLED_PROCESS);

        FacadeInteractionInsertDto newInteraction = new FacadeInteractionInsertDto();
        newInteraction.setCode("I.01");
        newInteraction.setName("Acelera");
        newInteraction.setInteractionType(CONTROL_ACTION);

        FacadeConnectionInsertDto newConnection = new FacadeConnectionInsertDto();
        newConnection.setCode("Cn.01");
        newConnection.setSourceCode("C.01");
        newConnection.setTargetCode("C.02");
        newConnection.setStyle(SOLID);
        newConnection.setInteractions(List.of(newInteraction));

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(newComp1, newComp2))
                .connections(List.of(newConnection))
                .build();

        // 2. Current State DB Mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of());

        // 3. Component Insertion Mocks
        ComponentReadDto compRead1 = ComponentReadDto.builder().id(generatedCompId1).code("C.01").build();
        ComponentReadDto compRead2 = ComponentReadDto.builder().id(generatedCompId2).code("C.02").build();

        when(componentService.insert(argThat(dto -> dto != null && "C.01".equals(dto.getCode())))).thenReturn(compRead1);
        when(componentService.insert(argThat(dto -> dto != null && "C.02".equals(dto.getCode())))).thenReturn(compRead2);

        // 4. Connection Insertion Mock
        ConnectionReadDto connRead = ConnectionReadDto.builder().id(generatedConnId).code("Cn.01").build();
        when(connectionService.insert(any(ConnectionInsertDto.class))).thenReturn(connRead);

        // Act
        service.saveControlStructure(payload);

        // Assert
        verify(microStampAuthClient, times(1)).getAnalysisById(analysisId);
        verify(componentService, times(2)).insert(any(ComponentInsertDto.class));
        verify(connectionService, times(1)).insert(any(ConnectionInsertDto.class));
        verify(interactionService, times(1)).insert(any(FacadeInteractionInsertDto.class));
    }

    @Test
    @DisplayName("When updating existing elements > Call update on services successfully")
    void WhenUpdatingExistingElementsCallUpdateOnServicesSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID compId = UUID.randomUUID();
        UUID connId = UUID.randomUUID();
        UUID interactionId = UUID.randomUUID();

        // Existing DTOs with ID
        FacadeComponentInsertDto existingComp = new FacadeComponentInsertDto();
        existingComp.setId(compId);
        existingComp.setCode("C.01");
        existingComp.setName("Motorista Atualizado");
        existingComp.setType(CONTROLLER);

        FacadeInteractionInsertDto existingInteraction = new FacadeInteractionInsertDto();
        existingInteraction.setId(interactionId);
        existingInteraction.setCode("I.01");
        existingInteraction.setName("Acelera Forte");
        existingInteraction.setInteractionType(CONTROL_ACTION);

        FacadeConnectionInsertDto existingConnection = new FacadeConnectionInsertDto();
        existingConnection.setId(connId);
        existingConnection.setCode("Cn.01");
        existingConnection.setSourceCode("C.01");
        existingConnection.setTargetCode("C.01");
        existingConnection.setStyle(SOLID);
        existingConnection.setInteractions(List.of(existingInteraction));

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(existingComp))
                .connections(List.of(existingConnection))
                .build();

        // Current Db state mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                InteractionReadDto.builder().id(interactionId).build()
        ));
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ConnectionReadDto.builder().id(connId).build()
        ));
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ComponentReadDto.builder().id(compId).code("C.01").build()
        ));

        // Act
        service.saveControlStructure(payload);

        // Assert
        verify(componentService, times(1)).update(eq(compId), any(ComponentUpdateDto.class));
        verify(connectionService, times(1)).update(eq(connId), any(ConnectionUpdateDto.class));
        verify(interactionService, times(1)).update(eq(interactionId), any());
    }

    @Test
    @DisplayName("When elements are omitted in payload > Clean up missing elements from database")
    void WhenElementsAreOmittedInPayloadCleanUpMissingElementsFromDatabase() {
        UUID analysisId = UUID.randomUUID();
        UUID oldCompId = UUID.randomUUID();
        UUID oldConnId = UUID.randomUUID();
        UUID oldInteractionId = UUID.randomUUID();

        // Empty payload sent
        ControlStructureInsertDto emptyPayload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of())
                .connections(List.of())
                .build();

        // Previous DB records
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                InteractionReadDto.builder().id(oldInteractionId).build()
        ));
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ConnectionReadDto.builder().id(oldConnId).build()
        ));
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ComponentReadDto.builder().id(oldCompId).build()
        ));

        // Act
        service.saveControlStructure(emptyPayload);

        // Assert
        verify(interactionService, times(1)).deleteDirectlyById(oldInteractionId);
        verify(connectionService, times(1)).delete(oldConnId);
        verify(componentService, times(1)).delete(oldCompId);
    }

    @Test
    @DisplayName("When existing connection source or target code is not found > Throw an exception")
    void WhenExistingConnectionSourceOrTargetCodeIsNotFoundThrowAnException() {
        UUID analysisId = UUID.randomUUID();
        UUID connId = UUID.randomUUID();

        // Connection is referencing an unexisting component "C.99"
        FacadeConnectionInsertDto invalidConnection = new FacadeConnectionInsertDto();
        invalidConnection.setId(connId);
        invalidConnection.setCode("Cn.01");
        invalidConnection.setSourceCode("C.99");
        invalidConnection.setTargetCode("C.01");
        invalidConnection.setStyle(SOLID);

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of())
                .connections(List.of(invalidConnection))
                .build();

        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of());

        assertThrows(Step2NotFoundException.class, () -> service.saveControlStructure(payload));
    }

    @Test
    @DisplayName("When father component is persisted in database and child is added in payload > Relate child to existing father successfully")
    void WhenFatherPersistedAndChildInPayloadRelatesChildToFatherSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID fatherCompId = UUID.randomUUID();
        UUID childCompId = UUID.randomUUID();

        // Father already persisted in the Db
        FacadeComponentInsertDto existingFather = new FacadeComponentInsertDto();
        existingFather.setId(fatherCompId);
        existingFather.setCode("C.01");
        existingFather.setName("Controlador");
        existingFather.setType(CONTROLLER);

        // New child referencing the father "C.01"
        FacadeComponentInsertDto newChild = new FacadeComponentInsertDto();
        newChild.setCode("C.02");
        newChild.setName("Atuador");
        newChild.setType(CONTROLLED_PROCESS);
        newChild.setFatherCode("C.01");

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(existingFather, newChild))
                .connections(List.of())
                .build();

        // Current Db state (Only father is persisted)
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ComponentReadDto.builder().id(fatherCompId).code("C.01").build()
        ));

        // child insert mock return
        ComponentReadDto childRead = ComponentReadDto.builder().id(childCompId).code("C.02").build();
        when(componentService.insert(argThat(dto -> dto != null && "C.02".equals(dto.getCode())))).thenReturn(childRead);

        // Act
        service.saveControlStructure(payload);

        // Assert
        verify(componentService, times(1)).update(eq(fatherCompId), any(ComponentUpdateDto.class));
        verify(componentService, times(1)).insert(argThat(dto -> dto != null && "C.02".equals(dto.getCode())));
        verify(componentService, never()).delete(any());
    }

    @Test
    @DisplayName("When two components are persisted in database > Relate one as father of the other on update successfully")
    void WhenTwoComponentsPersistedRelatesFatherAndChildOnUpdateSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID fatherCompId = UUID.randomUUID();
        UUID childCompId = UUID.randomUUID();

        // Father already in the Db
        FacadeComponentInsertDto existingFather = new FacadeComponentInsertDto();
        existingFather.setId(fatherCompId);
        existingFather.setCode("C.01");
        existingFather.setName("Controlador Principal");
        existingFather.setType(CONTROLLER);

        // Child persisted, updated to reference the father
        FacadeComponentInsertDto existingChild = new FacadeComponentInsertDto();
        existingChild.setId(childCompId);
        existingChild.setCode("C.02");
        existingChild.setName("Atuador Secundário");
        existingChild.setType(CONTROLLED_PROCESS);
        existingChild.setFatherCode("C.01");

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(existingFather, existingChild))
                .connections(List.of())
                .build();

        // Db mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ComponentReadDto.builder().id(fatherCompId).code("C.01").build(),
                ComponentReadDto.builder().id(childCompId).code("C.02").build()
        ));

        // Act
        service.saveControlStructure(payload);

        // Assert
        verify(componentService, times(1)).update(eq(fatherCompId), any(ComponentUpdateDto.class));
        verify(componentService, times(1)).update(eq(childCompId), any(ComponentUpdateDto.class));
        verify(componentService, never()).insert(any());
        verify(componentService, never()).delete(any());
    }

    @Test
    @DisplayName("When father and child components are both new in payload > Insert both and relate child to new father successfully")
    void WhenFatherAndChildInPayloadInsertsAndRelatesBothSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID generatedFatherId = UUID.randomUUID();
        UUID generatedChildId = UUID.randomUUID();

        // New Father
        FacadeComponentInsertDto newFather = new FacadeComponentInsertDto();
        newFather.setCode("C.01");
        newFather.setName("Novo Controlador");
        newFather.setType(CONTROLLER);

        // New child
        FacadeComponentInsertDto newChild = new FacadeComponentInsertDto();
        newChild.setCode("C.02");
        newChild.setName("Novo Atuador");
        newChild.setType(CONTROLLED_PROCESS);
        newChild.setFatherCode("C.01");

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(newFather, newChild))
                .connections(List.of())
                .build();

        // Empty Db
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of());

        ComponentReadDto fatherRead = ComponentReadDto.builder().id(generatedFatherId).code("C.01").build();
        ComponentReadDto childRead = ComponentReadDto.builder().id(generatedChildId).code("C.02").build();

        when(componentService.insert(argThat(dto -> dto != null && "C.01".equals(dto.getCode())))).thenReturn(fatherRead);
        when(componentService.insert(argThat(dto -> dto != null && "C.02".equals(dto.getCode())))).thenReturn(childRead);

        // Act
        service.saveControlStructure(payload);

        // Assert
        verify(componentService, times(2)).insert(any(ComponentInsertDto.class));
        verify(componentService, atLeastOnce()).update(any(), any(ComponentUpdateDto.class));
        verify(componentService, never()).delete(any());
    }

    @Test
    @DisplayName("When child component is persisted and new father is added in payload > Insert father and update child with new father relation successfully")
    void WhenChildPersistedAndFatherInPayloadInsertsFatherAndUpdateChildSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID generatedFatherId = UUID.randomUUID();
        UUID existingChildId = UUID.randomUUID();

        // New father in the payload
        FacadeComponentInsertDto newFather = new FacadeComponentInsertDto();
        newFather.setCode("C.01");
        newFather.setName("Novo Controlador Pai");
        newFather.setType(CONTROLLER);

        // Persisted child
        FacadeComponentInsertDto existingChild = new FacadeComponentInsertDto();
        existingChild.setId(existingChildId);
        existingChild.setCode("C.02");
        existingChild.setName("Atuador Existente");
        existingChild.setType(CONTROLLED_PROCESS);
        existingChild.setFatherCode("C.01");

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(newFather, existingChild))
                .connections(List.of())
                .build();

        // Db only contains child in the begining
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ComponentReadDto.builder().id(existingChildId).code("C.02").build()
        ));

        ComponentReadDto fatherRead = ComponentReadDto.builder().id(generatedFatherId).code("C.01").build();
        when(componentService.insert(argThat(dto -> dto != null && "C.01".equals(dto.getCode())))).thenReturn(fatherRead);

        // Act
        service.saveControlStructure(payload);

        // Assert
        verify(componentService, times(1)).insert(argThat(dto -> dto != null && "C.01".equals(dto.getCode())));
        verify(componentService, times(1)).update(eq(existingChildId), any(ComponentUpdateDto.class));
        verify(componentService, never()).delete(any());
    }

    @Test
    @DisplayName("When an existing connection source is altered > update connection source successfully")
    void WhenAConnectionSourceIsAlteredUpdateConnectionSuccessfully() {
        UUID analysisId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID oldSourceId = UUID.randomUUID();
        UUID newSourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        ComponentReadDto oldSourceComponentDto = ComponentReadDto.builder()
                .id(oldSourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto newSourceComponentDto = ComponentReadDto.builder()
                .id(newSourceId).code("C.02").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto targetComponentDto = ComponentReadDto.builder()
                .id(targetId).code("C.03").border(SOLID).isVisible(true).type("Controlled_Process").build();

        FacadeComponentInsertDto newSourceComponentInsertDto = FacadeComponentInsertDto.builder()
                .id(newSourceId).code("C.02").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto targetComponentInsertDto = FacadeComponentInsertDto.builder()
                .id(targetId).code("C.03").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        FacadeConnectionInsertDto connectionInsertDto = FacadeConnectionInsertDto.builder()
                .id(connectionId)
                .code("Cn.01")
                .sourceCode("C.02")
                .targetCode("C.03")
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(newSourceComponentInsertDto, targetComponentInsertDto))
                .connections(List.of(connectionInsertDto))
                .build();

        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());

        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ConnectionReadDto.builder()
                        .id(connectionId)
                        .code("Cn.01")
                        .source(oldSourceComponentDto)
                        .target(targetComponentDto)
                        .style(SOLID)
                        .interactions(List.of())
                        .build()
        ));

        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                oldSourceComponentDto, newSourceComponentDto, targetComponentDto
        ));

        service.saveControlStructure(payload);

        verify(connectionService, times(1)).update(eq(connectionId), argThat(dto ->
                dto != null && newSourceId.equals(dto.getSourceId())
        ));
    }

    @Test
    @DisplayName("When an existing connection target is altered > update connection target successfully")
    void WhenConnectionTargetIsAlteredShouldUpdateConnectionTargetSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID oldTargetId = UUID.randomUUID();
        UUID newTargetId = UUID.randomUUID();

        // Db reading Dtos
        ComponentReadDto sourceComponentDto = ComponentReadDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto oldTargetComponentDto = ComponentReadDto.builder()
                .id(oldTargetId).code("C.03").border(SOLID).isVisible(true).type("Controlled_Process").build();

        ComponentReadDto newTargetComponentDto = ComponentReadDto.builder()
                .id(newTargetId).code("C.04").border(SOLID).isVisible(true).type("Controlled_Process").build();

        // Payload
        FacadeComponentInsertDto sourceInsertDto = FacadeComponentInsertDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto newTargetInsertDto = FacadeComponentInsertDto.builder()
                .id(newTargetId).code("C.04").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        FacadeConnectionInsertDto connectionInsertDto = FacadeConnectionInsertDto.builder()
                .id(connectionId)
                .code("Cn.01")
                .sourceCode("C.01")
                .targetCode("C.04") // Referencing new target
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(sourceInsertDto, newTargetInsertDto))
                .connections(List.of(connectionInsertDto))
                .build();

        // Db mock
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());

        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ConnectionReadDto.builder()
                        .id(connectionId)
                        .code("Cn.01")
                        .source(sourceComponentDto)
                        .target(oldTargetComponentDto) // Antigo destino era C.03
                        .style(SOLID)
                        .interactions(List.of())
                        .build()
        ));

        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(
                sourceComponentDto, oldTargetComponentDto, newTargetComponentDto
        ));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(connectionService, times(1)).update(eq(connectionId), argThat(dto ->
                dto != null && newTargetId.equals(dto.getTargetId())
        ));
    }

    @Test
    @DisplayName("When component attributes are altered > update component successfully")
    void WhenComponentAttributesAreAlteredShouldUpdateComponentSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();

        // Db component
        ComponentReadDto existingComponentDto = ComponentReadDto.builder()
                .id(componentId)
                .code("C.01")
                .name("Motorista")
                .border(SOLID)
                .isVisible(true)
                .type("Controller")
                .build();

        // Payload component with a different name
        FacadeComponentInsertDto updatedComponentInsertDto = FacadeComponentInsertDto.builder()
                .id(componentId)
                .code("C.01")
                .name("Condutor Principal")
                .border(DASHED)
                .isVisible(true)
                .type(CONTROLLER)
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(updatedComponentInsertDto))
                .connections(List.of())
                .build();

        // Db mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(existingComponentDto));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        // validates if component received new name and border style
        verify(componentService, times(1)).update(eq(componentId), argThat(dto ->
                dto != null
                        && "Condutor Principal".equals(dto.getName())
                        && DASHED.equals(dto.getBorder())
        ));
    }

    @Test
    @DisplayName("When connection style is altered > update connection style successfully")
    void WhenConnectionStyleIsAlteredShouldUpdateConnectionStyleSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        ComponentReadDto sourceComponentDto = ComponentReadDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto targetComponentDto = ComponentReadDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type("Controlled_Process").build();

        FacadeComponentInsertDto sourceInsertDto = FacadeComponentInsertDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto targetInsertDto = FacadeComponentInsertDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        // Payload connection changed to Dashed style
        FacadeConnectionInsertDto connectionInsertDto = FacadeConnectionInsertDto.builder()
                .id(connectionId)
                .code("Cn.01")
                .sourceCode("C.01")
                .targetCode("C.02")
                .style(DASHED)
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(sourceInsertDto, targetInsertDto))
                .connections(List.of(connectionInsertDto))
                .build();

        // Db mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(
                ConnectionReadDto.builder()
                        .id(connectionId)
                        .code("Cn.01")
                        .source(sourceComponentDto)
                        .target(targetComponentDto)
                        .style(SOLID)
                        .interactions(List.of())
                        .build()
        ));
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(sourceComponentDto, targetComponentDto));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(connectionService, times(1)).update(eq(connectionId), argThat(dto ->
                dto != null && DASHED.equals(dto.getStyle())
        ));
    }

    @Test
    @DisplayName("When a component is removed from payload > delete component from database successfully")
    void WhenComponentIsRemovedFromPayloadShouldDeleteComponentSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID comp1Id = UUID.randomUUID();
        UUID comp2Id = UUID.randomUUID();
        UUID comp3Id = UUID.randomUUID(); // Component to be removed

        // DB has three components
        ComponentReadDto comp1Dto = ComponentReadDto.builder()
                .id(comp1Id).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto comp2Dto = ComponentReadDto.builder()
                .id(comp2Id).code("C.02").border(SOLID).isVisible(true).type("Controlled_Process").build();

        ComponentReadDto comp3Dto = ComponentReadDto.builder()
                .id(comp3Id).code("C.03").border(SOLID).isVisible(true).type("Controlled_Process").build();

        // Payload (missing the "C.03")
        FacadeComponentInsertDto comp1InsertDto = FacadeComponentInsertDto.builder()
                .id(comp1Id).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto comp2InsertDto = FacadeComponentInsertDto.builder()
                .id(comp2Id).code("C.02").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(comp1InsertDto, comp2InsertDto))
                .connections(List.of())
                .build();

        // Db mocks
        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(comp1Dto, comp2Dto, comp3Dto));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(componentService, times(1)).delete(comp3Id);
        verify(componentService, never()).delete(comp1Id);
        verify(componentService, never()).delete(comp2Id);
    }

    @Test
    @DisplayName("When a connection is removed from payload > delete connection from database successfully")
    void WhenConnectionIsRemovedFromPayloadShouldDeleteConnectionSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID conn1Id = UUID.randomUUID();
        UUID conn2Id = UUID.randomUUID(); // Connection to be removed

        ComponentReadDto sourceDto = ComponentReadDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto targetDto = ComponentReadDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type("Controlled_Process").build();

        FacadeComponentInsertDto sourceInsertDto = FacadeComponentInsertDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto targetInsertDto = FacadeComponentInsertDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        // Payload missing the connection to be removed
        FacadeConnectionInsertDto conn1InsertDto = FacadeConnectionInsertDto.builder()
                .id(conn1Id)
                .code("Cn.01")
                .sourceCode("C.01")
                .targetCode("C.02")
                .style(SOLID)
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(sourceInsertDto, targetInsertDto))
                .connections(List.of(conn1InsertDto))
                .build();

        // Db mocks
        ConnectionReadDto conn1ReadDto = ConnectionReadDto.builder()
                .id(conn1Id).code("Cn.01").source(sourceDto).target(targetDto).style(SOLID).interactions(List.of()).build();

        ConnectionReadDto conn2ReadDto = ConnectionReadDto.builder()
                .id(conn2Id).code("Cn.02").source(sourceDto).target(targetDto).style(SOLID).interactions(List.of()).build();

        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of());
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(conn1ReadDto, conn2ReadDto));
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(sourceDto, targetDto));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(connectionService, times(1)).delete(conn2Id);
        verify(connectionService, never()).delete(conn1Id);
    }

    @Test
    @DisplayName("When an interaction is removed from a connection > delete interaction from database successfully")
    void WhenInteractionIsRemovedFromConnectionShouldDeleteInteractionSuccessfully() {
        // ARRANGE
        UUID analysisId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID connId = UUID.randomUUID();
        UUID interactionId = UUID.randomUUID(); // Interaction to be deleted

        ComponentReadDto sourceDto = ComponentReadDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type("Controller").build();

        ComponentReadDto targetDto = ComponentReadDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type("Controlled_Process").build();

        FacadeComponentInsertDto sourceInsertDto = FacadeComponentInsertDto.builder()
                .id(sourceId).code("C.01").border(SOLID).isVisible(true).type(CONTROLLER).build();

        FacadeComponentInsertDto targetInsertDto = FacadeComponentInsertDto.builder()
                .id(targetId).code("C.02").border(SOLID).isVisible(true).type(CONTROLLED_PROCESS).build();

        // Payload
        FacadeConnectionInsertDto connectionInsertDto = FacadeConnectionInsertDto.builder()
                .id(connId)
                .code("Cn.01")
                .sourceCode("C.01")
                .targetCode("C.02")
                .style(SOLID)
                .interactions(List.of()) // Lista limpa
                .build();

        ControlStructureInsertDto payload = ControlStructureInsertDto.builder()
                .analysisId(analysisId)
                .components(List.of(sourceInsertDto, targetInsertDto))
                .connections(List.of(connectionInsertDto))
                .build();

        // Db mocks
        InteractionReadDto interactionReadDto = InteractionReadDto.builder()
                .id(interactionId)
                .code("IN.01")
                .build();

        ConnectionReadDto connectionReadDto = ConnectionReadDto.builder()
                .id(connId)
                .code("Cn.01")
                .source(sourceDto)
                .target(targetDto)
                .style(SOLID)
                .interactions(List.of(interactionReadDto))
                .build();

        when(interactionService.findByAnalysisId(analysisId)).thenReturn(List.of(interactionReadDto));
        when(connectionService.findByAnalysisId(analysisId)).thenReturn(List.of(connectionReadDto));
        when(componentService.findByAnalysisId(analysisId)).thenReturn(List.of(sourceDto, targetDto));

        // ACT
        service.saveControlStructure(payload);

        // ASSERT
        verify(interactionService, times(1)).deleteDirectlyById(interactionId);
    }

    // Fixture Suppliers
    private final Supplier<ControlStructureInsertDto> assembleEmptyControlStructureInsert = () ->
            ControlStructureInsertDto.builder()
                    .analysisId(UUID.randomUUID())
                    .components(new ArrayList<>())
                    .connections(new ArrayList<>())
                    .build();
}
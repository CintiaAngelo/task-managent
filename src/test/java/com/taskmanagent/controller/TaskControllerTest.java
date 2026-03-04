package com.taskmanagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagent.dto.CreateTaskRequest;
import com.taskmanagent.dto.TaskResponse;
import com.taskmanagent.dto.UpdateTaskRequest;
import com.taskmanagent.enums.TaskPriority;
import com.taskmanagent.enums.TaskStatus;
import com.taskmanagent.exception.BusinessValidationException;
import com.taskmanagent.exception.GlobalExceptionHandler;
import com.taskmanagent.exception.TaskNotEditableException;
import com.taskmanagent.exception.TaskNotFoundException;
import com.taskmanagent.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController - Testes Unitários")
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TaskResponse tarefaResponsePadrao;
    private String idTarefaPadrao;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        idTarefaPadrao = UUID.randomUUID().toString();
        tarefaResponsePadrao = TaskResponse.builder()
                .id(idTarefaPadrao)
                .title("Estudar Spring Boot")
                .description("Revisar DynamoDB")
                .status(TaskStatus.PENDING.getValue())
                .priority(TaskPriority.HIGH.getValue())
                .dueDate(LocalDate.now().plusDays(7).toString())
                .createdAt("2026-03-01T10:00:00")
                .updatedAt("2026-03-01T10:00:00")
                .build();
    }


    @Nested
    @DisplayName("POST /tasks - Criar Tarefa")
    class CriarTarefa {

        @Test
        @DisplayName("deve retornar 201 e tarefa criada quando dados são válidos")
        void deveRetornar201ETarefaCriadaQuandoDadosSaoValidos() throws Exception {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Estudar Spring Boot")
                    .description("Revisar DynamoDB")
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDate.now().plusDays(7).toString())
                    .build();

            when(taskService.criarTarefa(any(CreateTaskRequest.class))).thenReturn(tarefaResponsePadrao);

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(idTarefaPadrao))
                    .andExpect(jsonPath("$.title").value("Estudar Spring Boot"))
                    .andExpect(jsonPath("$.status").value("pending"));
        }

        @Test
        @DisplayName("deve retornar 400 quando título é vazio")
        void deveRetornar400QuandoTituloEVazio() throws Exception {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("")
                    .build();

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("deve retornar 400 quando título tem menos de 3 caracteres")
        void deveRetornar400QuandoTituloTemMenosDe3Caracteres() throws Exception {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("AB")
                    .build();

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando título não é enviado")
        void deveRetornar400QuandoTituloNaoEEnviado() throws Exception {
            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando prioridade inválida é enviada no JSON")
        void deveRetornar400QuandoPrioridadeInvalidaEnviadaNoJson() throws Exception {
            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Tarefa\", \"priority\": \"urgente\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando data de vencimento está no passado")
        void deveRetornar400QuandoDataVencimentoNoPassado() throws Exception {
            when(taskService.criarTarefa(any())).thenThrow(
                    new BusinessValidationException("Data de vencimento não pode ser no passado"));

            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa")
                    .dueDate("2020-01-01")
                    .build();

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Data de vencimento não pode ser no passado")));
        }

        @Test
        @DisplayName("deve retornar 400 quando título tem mais de 100 caracteres")
        void deveRetornar400QuandoTituloTemMaisDe100Caracteres() throws Exception {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("A".repeat(101))
                    .build();

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /tasks - Listar Tarefas")
    class ListarTarefas {

        @Test
        @DisplayName("deve retornar 200 e lista de tarefas quando chamado sem filtros")
        void deveRetornar200EListaDeTarefasQuandoChamadoSemFiltros() throws Exception {
            when(taskService.listarTarefas(null, null)).thenReturn(List.of(tarefaResponsePadrao));

            mockMvc.perform(get("/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(idTarefaPadrao));
        }

        @Test
        @DisplayName("deve retornar 200 e lista filtrada quando status é informado")
        void deveRetornar200EListaFiltradaQuandoStatusEInformado() throws Exception {
            when(taskService.listarTarefas("pending", null)).thenReturn(List.of(tarefaResponsePadrao));

            mockMvc.perform(get("/tasks").param("status", "pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("pending"));
        }

        @Test
        @DisplayName("deve retornar 200 e lista filtrada quando prioridade é informada")
        void deveRetornar200EListaFiltradaQuandoPrioridadeEInformada() throws Exception {
            when(taskService.listarTarefas(null, "high")).thenReturn(List.of(tarefaResponsePadrao));

            mockMvc.perform(get("/tasks").param("priority", "high"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].priority").value("high"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando não há tarefas")
        void deveRetornar200ComListaVaziaQuandoNaoHaTarefas() throws Exception {
            when(taskService.listarTarefas(null, null)).thenReturn(List.of());

            mockMvc.perform(get("/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("deve retornar 400 quando status informado é inválido")
        void deveRetornar400QuandoStatusInvalidoInformado() throws Exception {
            when(taskService.listarTarefas("invalido", null))
                    .thenThrow(new BusinessValidationException("Status inválido: 'invalido'"));

            mockMvc.perform(get("/tasks").param("status", "invalido"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("deve retornar 400 quando prioridade informada é inválida")
        void deveRetornar400QuandoPrioridadeInvalidaInformada() throws Exception {
            when(taskService.listarTarefas(null, "urgente"))
                    .thenThrow(new BusinessValidationException("Prioridade inválida: 'urgente'"));

            mockMvc.perform(get("/tasks").param("priority", "urgente"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("deve retornar 200 quando status e prioridade são informados juntos")
        void deveRetornar200QuandoStatusEPrioridadeInformadosJuntos() throws Exception {
            when(taskService.listarTarefas("pending", "high")).thenReturn(List.of(tarefaResponsePadrao));

            mockMvc.perform(get("/tasks")
                            .param("status", "pending")
                            .param("priority", "high"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("pending"));
        }
    }

    @Nested
    @DisplayName("GET /tasks/{id} - Buscar Tarefa por ID")
    class BuscarTarefaPorId {

        @Test
        @DisplayName("deve retornar 200 e tarefa quando ID existe")
        void deveRetornar200ETarefaQuandoIdExiste() throws Exception {
            when(taskService.buscarTarefaPorId(idTarefaPadrao)).thenReturn(tarefaResponsePadrao);

            mockMvc.perform(get("/tasks/{id}", idTarefaPadrao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idTarefaPadrao))
                    .andExpect(jsonPath("$.title").value("Estudar Spring Boot"));
        }

        @Test
        @DisplayName("deve retornar 404 quando ID não existe")
        void deveRetornar404QuandoIdNaoExiste() throws Exception {
            String idInexistente = "id-inexistente";
            when(taskService.buscarTarefaPorId(idInexistente))
                    .thenThrow(new TaskNotFoundException(idInexistente));

            mockMvc.perform(get("/tasks/{id}", idInexistente))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString(idInexistente)));
        }
    }

    @Nested
    @DisplayName("PUT /tasks/{id} - Atualizar Tarefa")
    class AtualizarTarefa {

        @Test
        @DisplayName("deve retornar 200 e tarefa atualizada quando dados são válidos")
        void deveRetornar200ETarefaAtualizadaQuandoDadosSaoValidos() throws Exception {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .title("Título Atualizado")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            TaskResponse responseAtualizado = TaskResponse.builder()
                    .id(idTarefaPadrao)
                    .title("Título Atualizado")
                    .status(TaskStatus.IN_PROGRESS.getValue())
                    .priority(TaskPriority.HIGH.getValue())
                    .build();

            when(taskService.atualizarTarefa(eq(idTarefaPadrao), any(UpdateTaskRequest.class)))
                    .thenReturn(responseAtualizado);

            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Título Atualizado"))
                    .andExpect(jsonPath("$.status").value("in_progress"));
        }

        @Test
        @DisplayName("deve retornar 404 ao atualizar tarefa com ID inexistente")
        void deveRetornar404AoAtualizarTarefaComIdInexistente() throws Exception {
            String idInexistente = "nao-existe";
            UpdateTaskRequest request = UpdateTaskRequest.builder().title("Novo título").build();

            when(taskService.atualizarTarefa(eq(idInexistente), any()))
                    .thenThrow(new TaskNotFoundException(idInexistente));

            mockMvc.perform(put("/tasks/{id}", idInexistente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 ao tentar editar tarefa com status completed")
        void deveRetornar422AoTentarEditarTarefaComStatusCompleted() throws Exception {
            UpdateTaskRequest request = UpdateTaskRequest.builder().title("Nova tentativa").build();

            when(taskService.atualizarTarefa(eq(idTarefaPadrao), any()))
                    .thenThrow(new TaskNotEditableException(idTarefaPadrao));

            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }

        @Test
        @DisplayName("deve retornar 400 quando título de atualização tem menos de 3 caracteres")
        void deveRetornar400QuandoTituloDeAtualizacaoTemMenosDe3Caracteres() throws Exception {
            UpdateTaskRequest request = UpdateTaskRequest.builder().title("AB").build();

            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando status inválido é enviado no JSON")
        void deveRetornar400QuandoStatusInvalidoEnviadoNoJson() throws Exception {
            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"inexistente\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando prioridade inválida é enviada no JSON")
        void deveRetornar400QuandoPrioridadeInvalidaEnviadaNoJson() throws Exception {
            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Título\", \"priority\": \"urgente\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 200 ao editar tarefa com status cancelled")
        void deveRetornar200AoEditarTarefaComStatusCancelled() throws Exception {
            UpdateTaskRequest request = UpdateTaskRequest.builder().title("Título editado").build();

            TaskResponse responseAtualizado = TaskResponse.builder()
                    .id(idTarefaPadrao)
                    .title("Título editado")
                    .status(TaskStatus.CANCELLED.getValue())
                    .priority(TaskPriority.HIGH.getValue())
                    .build();

            when(taskService.atualizarTarefa(eq(idTarefaPadrao), any()))
                    .thenReturn(responseAtualizado);

            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));
        }

        @Test
        @DisplayName("deve retornar 400 quando data de vencimento está no passado")
        void deveRetornar400QuandoDataVencimentoNoPassado() throws Exception {
            when(taskService.atualizarTarefa(eq(idTarefaPadrao), any()))
                    .thenThrow(new BusinessValidationException("Data de vencimento não pode ser no passado"));

            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .dueDate("2019-06-15")
                    .build();

            mockMvc.perform(put("/tasks/{id}", idTarefaPadrao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }


    @Nested
    @DisplayName("DELETE /tasks/{id} - Deletar Tarefa")
    class DeletarTarefa {

        @Test
        @DisplayName("deve retornar 204 quando tarefa é deletada com sucesso")
        void deveRetornar204QuandoTarefaEDeletadaComSucesso() throws Exception {
            doNothing().when(taskService).deletarTarefa(idTarefaPadrao);

            mockMvc.perform(delete("/tasks/{id}", idTarefaPadrao))
                    .andExpect(status().isNoContent());

            verify(taskService, times(1)).deletarTarefa(idTarefaPadrao);
        }

        @Test
        @DisplayName("deve retornar 404 ao tentar deletar tarefa com ID inexistente")
        void deveRetornar404AoTentarDeletarTarefaComIdInexistente() throws Exception {
            String idInexistente = "nao-existe";
            doThrow(new TaskNotFoundException(idInexistente)).when(taskService).deletarTarefa(idInexistente);

            mockMvc.perform(delete("/tasks/{id}", idInexistente))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
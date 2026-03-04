package com.taskmanagent.service;

import com.taskmanagent.dto.CreateTaskRequest;
import com.taskmanagent.dto.TaskResponse;
import com.taskmanagent.dto.UpdateTaskRequest;
import com.taskmanagent.entity.Task;
import com.taskmanagent.enums.TaskPriority;
import com.taskmanagent.enums.TaskStatus;
import com.taskmanagent.exception.BusinessValidationException;
import com.taskmanagent.exception.TaskNotEditableException;
import com.taskmanagent.exception.TaskNotFoundException;
import com.taskmanagent.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService - Testes Unitários")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task tarefaPadrao;
    private String idTarefaPadrao;

    @BeforeEach
    void configurar() {
        idTarefaPadrao = UUID.randomUUID().toString();
        tarefaPadrao = Task.builder()
                .id(idTarefaPadrao)
                .title("Estudar Spring Boot")
                .description("Revisar conceitos de DynamoDB")
                .status(TaskStatus.PENDING.getValue())
                .priority(TaskPriority.HIGH.getValue())
                .dueDate(LocalDate.now().plusDays(7).toString())
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
    }


    @Nested
    @DisplayName("Criar Tarefa")
    class CriarTarefa {

        @Test
        @DisplayName("deve criar tarefa com sucesso quando dados são válidos")
        void deveCriarTarefaComSucessoQuandoDadosSaoValidos() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Estudar Spring Boot")
                    .description("Revisar DynamoDB")
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDate.now().plusDays(7).toString())
                    .build();

            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaPadrao);

            TaskResponse response = taskService.criarTarefa(request);

            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Estudar Spring Boot");
            assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING.getValue());
            verify(taskRepository, times(1)).salvar(any(Task.class));
        }

        @Test
        @DisplayName("deve criar tarefa com prioridade medium quando prioridade não é informada")
        void deveCriarTarefaComPrioridadeMediumQuandoPrioridadeNaoEInformada() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa sem prioridade")
                    .build();

            Task tarefaSemPrioridade = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Tarefa sem prioridade")
                    .status(TaskStatus.PENDING.getValue())
                    .priority(TaskPriority.MEDIUM.getValue())
                    .createdAt(LocalDateTime.now().toString())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaSemPrioridade);

            TaskResponse response = taskService.criarTarefa(request);

            assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM.getValue());
        }

        @Test
        @DisplayName("deve lançar exceção quando data de vencimento está no passado")
        void deveLancarExcecaoQuandoDataDeVencimentoEstaNoPassado() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa com data passada")
                    .dueDate("2020-01-01")
                    .build();

            assertThatThrownBy(() -> taskService.criarTarefa(request))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Data de vencimento não pode ser no passado");

            verify(taskRepository, never()).salvar(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando formato da data é inválido")
        void deveLancarExcecaoQuandoFormatoDaDataEInvalido() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa com data inválida")
                    .dueDate("31/12/2099")
                    .build();

            assertThatThrownBy(() -> taskService.criarTarefa(request))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Formato de data inválido");

            verify(taskRepository, never()).salvar(any());
        }

        @Test
        @DisplayName("deve criar tarefa sem data de vencimento quando não informada")
        void deveCriarTarefaSemDataDeVencimentoQuandoNaoInformada() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa sem data")
                    .build();

            Task tarefaSemData = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Tarefa sem data")
                    .status(TaskStatus.PENDING.getValue())
                    .priority(TaskPriority.MEDIUM.getValue())
                    .createdAt(LocalDateTime.now().toString())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaSemData);

            TaskResponse response = taskService.criarTarefa(request);

            assertThat(response).isNotNull();
            assertThat(response.getDueDate()).isNull();
        }

        @Test
        @DisplayName("deve criar tarefa com status PENDING independente do que for enviado")
        void deveCriarTarefaComStatusPendingPorPadrao() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa nova")
                    .build();

            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaPadrao);

            TaskResponse response = taskService.criarTarefa(request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING.getValue());
        }

        @Test
        @DisplayName("deve criar tarefa com prioridade LOW quando informada")
        void deveCriarTarefaComPrioridadeLow() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .title("Tarefa com baixa prioridade")
                    .priority(TaskPriority.LOW)
                    .build();

            Task tarefaLow = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Tarefa com baixa prioridade")
                    .status(TaskStatus.PENDING.getValue())
                    .priority(TaskPriority.LOW.getValue())
                    .createdAt(LocalDateTime.now().toString())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaLow);

            TaskResponse response = taskService.criarTarefa(request);

            assertThat(response.getPriority()).isEqualTo(TaskPriority.LOW.getValue());
        }
    }

    @Nested
    @DisplayName("Listar Tarefas")
    class ListarTarefas {

        @Test
        @DisplayName("deve retornar todas as tarefas quando nenhum filtro é informado")
        void deveRetornarTodasAsTarefasQuandoNenhumFiltroEInformado() {
            when(taskRepository.listarTodas()).thenReturn(List.of(tarefaPadrao));

            List<TaskResponse> tarefas = taskService.listarTarefas(null, null);

            assertThat(tarefas).hasSize(1);
            assertThat(tarefas.get(0).getId()).isEqualTo(idTarefaPadrao);
            verify(taskRepository, times(1)).listarTodas();
            verify(taskRepository, never()).listarPorStatus(any());
            verify(taskRepository, never()).listarPorPrioridade(any());
        }

        @Test
        @DisplayName("deve filtrar tarefas por status quando status é informado")
        void deveFiltrarTarefasPorStatusQuandoStatusEInformado() {
            when(taskRepository.listarPorStatus("pending")).thenReturn(List.of(tarefaPadrao));

            List<TaskResponse> tarefas = taskService.listarTarefas("pending", null);

            assertThat(tarefas).hasSize(1);
            assertThat(tarefas.get(0).getStatus()).isEqualTo("pending");
            verify(taskRepository, times(1)).listarPorStatus("pending");
        }

        @Test
        @DisplayName("deve filtrar tarefas por prioridade quando prioridade é informada")
        void deveFiltrarTarefasPorPrioridadeQuandoPrioridadeEInformada() {
            when(taskRepository.listarPorPrioridade("high")).thenReturn(List.of(tarefaPadrao));

            List<TaskResponse> tarefas = taskService.listarTarefas(null, "high");

            assertThat(tarefas).hasSize(1);
            assertThat(tarefas.get(0).getPriority()).isEqualTo("high");
            verify(taskRepository, times(1)).listarPorPrioridade("high");
        }

        @Test
        @DisplayName("deve lançar exceção quando status informado é inválido")
        void deveLancarExcecaoQuandoStatusInformadoEInvalido() {
            assertThatThrownBy(() -> taskService.listarTarefas("invalido", null))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Status inválido");
        }

        @Test
        @DisplayName("deve lançar exceção quando prioridade informada é inválida")
        void deveLancarExcecaoQuandoPrioridadeInformadaEInvalida() {
            assertThatThrownBy(() -> taskService.listarTarefas(null, "urgente"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Prioridade inválida");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há tarefas cadastradas")
        void deveRetornarListaVaziaQuandoNaoHaTarefasCadastradas() {
            when(taskRepository.listarTodas()).thenReturn(List.of());

            List<TaskResponse> tarefas = taskService.listarTarefas(null, null);

            assertThat(tarefas).isEmpty();
        }

        @Test
        @DisplayName("deve priorizar filtro por status quando status e prioridade são informados juntos")
        void devePriorizarFiltroStatusQuandoAmbosInformados() {
            when(taskRepository.listarPorStatus("pending")).thenReturn(List.of(tarefaPadrao));

            List<TaskResponse> tarefas = taskService.listarTarefas("pending", "high");

            assertThat(tarefas).hasSize(1);
            verify(taskRepository, times(1)).listarPorStatus("pending");
            verify(taskRepository, never()).listarPorPrioridade(any());
        }

        @Test
        @DisplayName("deve ignorar filtro em branco e retornar todas as tarefas")
        void deveIgnorarFiltroEmBrancoERetornarTodas() {
            when(taskRepository.listarTodas()).thenReturn(List.of(tarefaPadrao));

            List<TaskResponse> tarefas = taskService.listarTarefas("  ", "  ");

            assertThat(tarefas).hasSize(1);
            verify(taskRepository, times(1)).listarTodas();
        }
    }

    @Nested
    @DisplayName("Buscar Tarefa por ID")
    class BuscarTarefaPorId {

        @Test
        @DisplayName("deve retornar tarefa quando ID existe")
        void deveRetornarTarefaQuandoIdExiste() {
            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));

            TaskResponse response = taskService.buscarTarefaPorId(idTarefaPadrao);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(idTarefaPadrao);
            assertThat(response.getTitle()).isEqualTo("Estudar Spring Boot");
        }

        @Test
        @DisplayName("deve lançar TaskNotFoundException quando ID não existe")
        void deveLancarTaskNotFoundExceptionQuandoIdNaoExiste() {
            String idInexistente = "id-que-nao-existe";
            when(taskRepository.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.buscarTarefaPorId(idInexistente))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining(idInexistente);
        }

        @Test
        @DisplayName("deve retornar todos os campos corretamente mapeados")
        void deveRetornarTodosOsCamposCorretamenteMapeados() {
            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));

            TaskResponse response = taskService.buscarTarefaPorId(idTarefaPadrao);

            assertThat(response.getId()).isEqualTo(tarefaPadrao.getId());
            assertThat(response.getTitle()).isEqualTo(tarefaPadrao.getTitle());
            assertThat(response.getDescription()).isEqualTo(tarefaPadrao.getDescription());
            assertThat(response.getStatus()).isEqualTo(tarefaPadrao.getStatus());
            assertThat(response.getPriority()).isEqualTo(tarefaPadrao.getPriority());
            assertThat(response.getDueDate()).isEqualTo(tarefaPadrao.getDueDate());
            assertThat(response.getCreatedAt()).isEqualTo(tarefaPadrao.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(tarefaPadrao.getUpdatedAt());
        }
    }


    @Nested
    @DisplayName("Atualizar Tarefa")
    class AtualizarTarefa {

        @Test
        @DisplayName("deve atualizar título da tarefa com sucesso")
        void deveAtualizarTituloDaTarefaComSucesso() {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .title("Título Atualizado")
                    .build();

            Task tarefaAtualizada = Task.builder()
                    .id(idTarefaPadrao)
                    .title("Título Atualizado")
                    .status(TaskStatus.PENDING.getValue())
                    .priority(TaskPriority.HIGH.getValue())
                    .createdAt(tarefaPadrao.getCreatedAt())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaAtualizada);

            TaskResponse response = taskService.atualizarTarefa(idTarefaPadrao, request);

            assertThat(response.getTitle()).isEqualTo("Título Atualizado");
            verify(taskRepository, times(1)).salvar(any(Task.class));
        }

        @Test
        @DisplayName("deve atualizar status da tarefa para in_progress com sucesso")
        void deveAtualizarStatusDaTarefaParaInProgressComSucesso() {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            Task tarefaAtualizada = Task.builder()
                    .id(idTarefaPadrao)
                    .title(tarefaPadrao.getTitle())
                    .status(TaskStatus.IN_PROGRESS.getValue())
                    .priority(tarefaPadrao.getPriority())
                    .createdAt(tarefaPadrao.getCreatedAt())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaAtualizada);

            TaskResponse response = taskService.atualizarTarefa(idTarefaPadrao, request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS.getValue());
        }

        @Test
        @DisplayName("deve lançar TaskNotEditableException ao tentar editar tarefa com status completed")
        void deveLancarTaskNotEditableExceptionAoTentarEditarTarefaComStatusCompleted() {
            tarefaPadrao.setStatus(TaskStatus.COMPLETED.getValue());

            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .title("Tentativa de edição")
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));

            assertThatThrownBy(() -> taskService.atualizarTarefa(idTarefaPadrao, request))
                    .isInstanceOf(TaskNotEditableException.class)
                    .hasMessageContaining(idTarefaPadrao);

            verify(taskRepository, never()).salvar(any());
        }

        @Test
        @DisplayName("deve lançar TaskNotFoundException ao tentar atualizar tarefa inexistente")
        void deveLancarTaskNotFoundExceptionAoTentarAtualizarTarefaInexistente() {
            String idInexistente = "id-inexistente";
            when(taskRepository.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            UpdateTaskRequest request = UpdateTaskRequest.builder().title("Novo título").build();

            assertThatThrownBy(() -> taskService.atualizarTarefa(idInexistente, request))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining(idInexistente);
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar tarefa com data de vencimento no passado")
        void deveLancarExcecaoAoAtualizarTarefaComDataDeVencimentoNoPassado() {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .dueDate("2019-06-15")
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));

            assertThatThrownBy(() -> taskService.atualizarTarefa(idTarefaPadrao, request))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Data de vencimento não pode ser no passado");
        }

        @Test
        @DisplayName("deve manter campos não alterados quando apenas alguns campos são atualizados")
        void deveManterCamposNaoAlteradosQuandoApenaAlgunsCamposSaoAtualizados() {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .title("Novo Título")
                    .build();

            Task tarefaAtualizada = Task.builder()
                    .id(idTarefaPadrao)
                    .title("Novo Título")
                    .description(tarefaPadrao.getDescription())
                    .status(tarefaPadrao.getStatus())
                    .priority(tarefaPadrao.getPriority())
                    .dueDate(tarefaPadrao.getDueDate())
                    .createdAt(tarefaPadrao.getCreatedAt())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaAtualizada);

            TaskResponse response = taskService.atualizarTarefa(idTarefaPadrao, request);

            assertThat(response.getTitle()).isEqualTo("Novo Título");
            assertThat(response.getDescription()).isEqualTo(tarefaPadrao.getDescription());
            assertThat(response.getStatus()).isEqualTo(tarefaPadrao.getStatus());
        }

        @Test
        @DisplayName("deve permitir editar tarefa com status cancelled")
        void devePermitirEditarTarefaComStatusCancelled() {
            tarefaPadrao.setStatus(TaskStatus.CANCELLED.getValue());

            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .title("Título editado")
                    .build();

            Task tarefaAtualizada = Task.builder()
                    .id(idTarefaPadrao)
                    .title("Título editado")
                    .status(TaskStatus.CANCELLED.getValue())
                    .priority(tarefaPadrao.getPriority())
                    .createdAt(tarefaPadrao.getCreatedAt())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaAtualizada);

            TaskResponse response = taskService.atualizarTarefa(idTarefaPadrao, request);

            assertThat(response.getTitle()).isEqualTo("Título editado");
            verify(taskRepository, times(1)).salvar(any(Task.class));
        }

        @Test
        @DisplayName("deve atualizar apenas a prioridade mantendo os demais campos")
        void deveAtualizarApenasPrioridadeMantentoOsDemaisCampos() {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .priority(TaskPriority.LOW)
                    .build();

            Task tarefaAtualizada = Task.builder()
                    .id(idTarefaPadrao)
                    .title(tarefaPadrao.getTitle())
                    .description(tarefaPadrao.getDescription())
                    .status(tarefaPadrao.getStatus())
                    .priority(TaskPriority.LOW.getValue())
                    .dueDate(tarefaPadrao.getDueDate())
                    .createdAt(tarefaPadrao.getCreatedAt())
                    .updatedAt(LocalDateTime.now().toString())
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            when(taskRepository.salvar(any(Task.class))).thenReturn(tarefaAtualizada);

            TaskResponse response = taskService.atualizarTarefa(idTarefaPadrao, request);

            assertThat(response.getPriority()).isEqualTo(TaskPriority.LOW.getValue());
            assertThat(response.getTitle()).isEqualTo(tarefaPadrao.getTitle());
            assertThat(response.getStatus()).isEqualTo(tarefaPadrao.getStatus());
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar tarefa com formato de data inválido")
        void deveLancarExcecaoAoAtualizarComFormatoDataInvalido() {
            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .dueDate("31/12/2099")
                    .build();

            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));

            assertThatThrownBy(() -> taskService.atualizarTarefa(idTarefaPadrao, request))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Formato de data inválido");

            verify(taskRepository, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Deletar Tarefa")
    class DeletarTarefa {

        @Test
        @DisplayName("deve deletar tarefa com sucesso quando ID existe")
        void deveDeletarTarefaComSucessoQuandoIdExiste() {
            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            doNothing().when(taskRepository).deletar(idTarefaPadrao);

            assertThatCode(() -> taskService.deletarTarefa(idTarefaPadrao))
                    .doesNotThrowAnyException();

            verify(taskRepository, times(1)).deletar(idTarefaPadrao);
        }

        @Test
        @DisplayName("deve lançar TaskNotFoundException ao tentar deletar tarefa inexistente")
        void deveLancarTaskNotFoundExceptionAoTentarDeletarTarefaInexistente() {
            String idInexistente = "id-inexistente";
            when(taskRepository.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deletarTarefa(idInexistente))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining(idInexistente);

            verify(taskRepository, never()).deletar(any());
        }

        @Test
        @DisplayName("deve permitir deletar tarefa com status completed")
        void devePermitirDeletarTarefaComStatusCompleted() {
            tarefaPadrao.setStatus(TaskStatus.COMPLETED.getValue());
            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            doNothing().when(taskRepository).deletar(idTarefaPadrao);

            assertThatCode(() -> taskService.deletarTarefa(idTarefaPadrao))
                    .doesNotThrowAnyException();

            verify(taskRepository, times(1)).deletar(idTarefaPadrao);
        }

        @Test
        @DisplayName("deve permitir deletar tarefa com status cancelled")
        void devePermitirDeletarTarefaComStatusCancelled() {
            tarefaPadrao.setStatus(TaskStatus.CANCELLED.getValue());
            when(taskRepository.buscarPorId(idTarefaPadrao)).thenReturn(Optional.of(tarefaPadrao));
            doNothing().when(taskRepository).deletar(idTarefaPadrao);

            assertThatCode(() -> taskService.deletarTarefa(idTarefaPadrao))
                    .doesNotThrowAnyException();

            verify(taskRepository, times(1)).deletar(idTarefaPadrao);
        }
    }
}
package com.taskmanagent.service;


import com.taskmanagent.dto.CreateTaskRequest;
import com.taskmanagent.dto.TaskResponse;
import com.taskmanagent.dto.TaskSummaryResponse;
import com.taskmanagent.dto.UpdateTaskRequest;
import com.taskmanagent.entity.Task;
import com.taskmanagent.enums.TaskPriority;
import com.taskmanagent.enums.TaskStatus;
import com.taskmanagent.exception.BusinessValidationException;
import com.taskmanagent.exception.TaskNotEditableException;
import com.taskmanagent.exception.TaskNotFoundException;
import com.taskmanagent.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskResponse criarTarefa(CreateTaskRequest request) {
        log.info("Criando nova tarefa com título: {}", request.getTitle());

        validarDataVencimento(request.getDueDate());

        String prioridade = request.getPriority() != null
                ? request.getPriority().getValue()
                : TaskPriority.MEDIUM.getValue();

        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING.getValue())
                .priority(prioridade)
                .dueDate(request.getDueDate())
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();

        Task taskSalva = taskRepository.salvar(task);
        log.info("Tarefa criada com sucesso. ID: {}", taskSalva.getId());

        return mapearParaResponse(taskSalva);
    }

    public List<TaskResponse> listarTarefas(String status, String priority) {
        log.info("Listando tarefas. Filtro status: {}, Filtro priority: {}", status, priority);

        List<Task> tarefas;

        if (status != null && !status.isBlank()) {
            validarStatus(status);
            tarefas = taskRepository.listarPorStatus(status);
        } else if (priority != null && !priority.isBlank()) {
            validarPrioridade(priority);
            tarefas = taskRepository.listarPorPrioridade(priority);
        } else {
            tarefas = taskRepository.listarTodas();
        }

        return tarefas.stream()
                .map(this::mapearParaResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse buscarTarefaPorId(String id) {
        log.info("Buscando tarefa com ID: {}", id);
        Task task = taskRepository.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return mapearParaResponse(task);
    }
    public TaskSummaryResponse resumirPorStatus() {
        List<Task> tarefas = taskRepository.listarTodas();

        return TaskSummaryResponse.builder()
                .pending(contarPorStatus(tarefas, TaskStatus.PENDING))
                .in_progress(contarPorStatus(tarefas, TaskStatus.IN_PROGRESS))
                .completed(contarPorStatus(tarefas, TaskStatus.COMPLETED))
                .cancelled(contarPorStatus(tarefas, TaskStatus.CANCELLED))
                .build();
    }

    private long contarPorStatus(List<Task> tarefas, TaskStatus status) {
        return tarefas.stream()
                .filter(t -> status.getValue().equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public TaskResponse atualizarTarefa(String id, UpdateTaskRequest request) {
        log.info("Atualizando tarefa com ID: {}", id);

        Task task = taskRepository.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        if (TaskStatus.COMPLETED.getValue().equalsIgnoreCase(task.getStatus())) {
            throw new TaskNotEditableException(id);
        }

        if (request.getDueDate() != null) {
            validarDataVencimento(request.getDueDate());
        }

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus().getValue());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority().getValue());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        task.setUpdatedAt(LocalDateTime.now().toString());

        Task taskAtualizada = taskRepository.salvar(task);
        log.info("Tarefa atualizada com sucesso. ID: {}", taskAtualizada.getId());

        return mapearParaResponse(taskAtualizada);
    }

    public void deletarTarefa(String id) {
        log.info("Deletando tarefa com ID: {}", id);

        taskRepository.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        taskRepository.deletar(id);
        log.info("Tarefa deletada com sucesso. ID: {}", id);
    }

    private void validarDataVencimento(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) {
            return;
        }
        try {
            LocalDate data = LocalDate.parse(dueDate);
            if (data.isBefore(LocalDate.now())) {
                throw new BusinessValidationException(
                        "Data de vencimento não pode ser no passado. Data informada: " + dueDate
                );
            }
        } catch (DateTimeParseException e) {
            throw new BusinessValidationException(
                    "Formato de data inválido: '" + dueDate + "'. Use o formato yyyy-MM-dd"
            );
        }
    }

    private void validarStatus(String status) {
        try {
            TaskStatus.fromValue(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException(e.getMessage());
        }
    }

    private void validarPrioridade(String priority) {
        try {
            TaskPriority.fromValue(priority);
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException(e.getMessage());
        }
    }

    private TaskResponse mapearParaResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

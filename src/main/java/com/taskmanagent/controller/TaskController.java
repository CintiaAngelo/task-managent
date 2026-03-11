package com.taskmanagent.controller;

import com.taskmanagent.dto.*;
import com.taskmanagent.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "API para gerenciamento de tarefas")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Criar nova tarefa", description = "Cria uma nova tarefa com os dados fornecidos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> criarTarefa(@Valid @RequestBody CreateTaskRequest request) {
        log.info("POST /tasks - Criando tarefa: {}", request.getTitle());
        TaskResponse response = taskService.criarTarefa(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar tarefas", description = "Lista todas as tarefas, com filtro opcional por status ou prioridade")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de tarefas retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetro de filtro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<TaskResponse>> listarTarefas(
            @Parameter(description = "Filtrar por status: pending, in_progress, completed, cancelled")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtrar por prioridade: low, medium, high")
            @RequestParam(required = false) String priority) {
        log.info("GET /tasks - status={}, priority={}", status, priority);
        List<TaskResponse> tarefas = taskService.listarTarefas(status, priority);
        return ResponseEntity.ok(tarefas);
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo por status", description = "Retorna a quantidade de tarefas por status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = TaskSummaryResponse.class)))
    })
    public ResponseEntity<TaskSummaryResponse> resumirPorStatus() {
        log.info("GET /tasks/summary - Buscando resumo por status");
        TaskSummaryResponse response = taskService.resumirPorStatus();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tarefa por ID", description = "Retorna uma tarefa específica pelo seu identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarefa encontrada"),
            @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> buscarPorId(
            @Parameter(description = "ID único da tarefa") @PathVariable String id) {
        log.info("GET /tasks/{} - Buscando tarefa", id);
        TaskResponse response = taskService.buscarTarefaPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tarefa", description = "Atualiza os dados de uma tarefa existente. Tarefas com status 'completed' não podem ser editadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Tarefa não pode ser editada (status completed)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> atualizarTarefa(
            @Parameter(description = "ID único da tarefa") @PathVariable String id,
            @Valid @RequestBody UpdateTaskRequest request) {
        log.info("PUT /tasks/{} - Atualizando tarefa", id);
        TaskResponse response = taskService.atualizarTarefa(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar tarefa", description = "Remove permanentemente uma tarefa pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tarefa deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletarTarefa(
            @Parameter(description = "ID único da tarefa") @PathVariable String id) {
        log.info("DELETE /tasks/{} - Deletando tarefa", id);
        taskService.deletarTarefa(id);
        return ResponseEntity.noContent().build();
    }
}


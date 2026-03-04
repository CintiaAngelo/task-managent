package com.taskmanagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta de erro padronizada")
public class ErrorResponse {

    @Schema(description = "Código de status HTTP", example = "400")
    private int status;

    @Schema(description = "Mensagem de erro principal", example = "Dados inválidos")
    private String message;

    @Schema(description = "Timestamp do erro")
    private String timestamp;

    @Schema(description = "Lista de erros de validação")
    private List<String> errors;

    public static ErrorResponse of(int status, String message) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    public static ErrorResponse ofValidation(int status, String message, List<String> errors) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .errors(errors)
                .build();
    }
}

package com.bank.digital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @Schema(description = "Código HTTP do erro", example = "400")
    private int status;

    @Schema(description = "Mensagem principal do erro", example = "Saldo insuficiente")
    private String message;

    @Schema(description = "Timestamp do erro")
    private LocalDateTime timestamp;

    @Schema(description = "Detalhes adicionais (erros de validação)")
    private List<String> details;
}

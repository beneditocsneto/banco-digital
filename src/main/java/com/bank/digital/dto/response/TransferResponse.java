package com.bank.digital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    @Schema(description = "ID interno da transação", example = "10")
    private Long id;

    @Schema(description = "Identificador externo (idempotência)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String externalId;

    @Schema(description = "Número da conta de origem", example = "1001")
    private String sourceAccountNumber;

    @Schema(description = "Número da conta de destino", example = "1002")
    private String targetAccountNumber;

    @Schema(description = "Valor transferido", example = "250.00")
    private BigDecimal amount;

    @Schema(description = "Moeda", example = "BRL")
    private String currency;

    @Schema(description = "Status da transação", example = "COMPLETED")
    private String status;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Schema(description = "Data de conclusão")
    private LocalDateTime completedAt;
}

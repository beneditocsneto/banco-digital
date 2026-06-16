package com.bank.digital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotBlank(message = "O número da conta de origem é obrigatório")
    @Schema(description = "Número da conta de origem", example = "1001")
    private String sourceAccountNumber;

    @NotBlank(message = "O número da conta de destino é obrigatório")
    @Schema(description = "Número da conta de destino", example = "1002")
    private String targetAccountNumber;

    @NotNull(message = "O valor da transferência é obrigatório")
    @Positive(message = "O valor da transferência deve ser positivo")
    @Schema(description = "Valor a ser transferido", example = "250.00")
    private BigDecimal amount;

    @NotBlank(message = "A moeda é obrigatória")
    @Schema(description = "Moeda (ISO 4217)", example = "BRL")
    private String currency;

    @NotBlank(message = "O identificador externo é obrigatório (idempotência)")
    @Schema(description = "Identificador único para idempotência (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String externalId;
}

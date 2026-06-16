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
public class AccountResponse {

    @Schema(description = "ID interno da conta", example = "1")
    private Long id;

    @Schema(description = "Número da conta", example = "1001")
    private String accountNumber;

    @Schema(description = "Nome do titular", example = "João Silva")
    private String holderName;

    @Schema(description = "Saldo atual", example = "5000.00")
    private BigDecimal balance;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;
}

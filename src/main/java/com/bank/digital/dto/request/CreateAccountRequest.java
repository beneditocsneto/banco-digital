package com.bank.digital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "O número da conta é obrigatório")
    @Schema(description = "Número da conta", example = "1010")
    private String accountNumber;

    @NotBlank(message = "O nome do titular é obrigatório")
    @Schema(description = "Nome do titular da conta", example = "Benedito Cardoso")
    private String holderName;

    @PositiveOrZero(message = "O saldo inicial não pode ser negativo")
    @Schema(description = "Saldo inicial da conta", example = "10000.00")
    private BigDecimal initialBalance;
}

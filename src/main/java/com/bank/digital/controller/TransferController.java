package com.bank.digital.controller;

import com.bank.digital.dto.request.TransferRequest;
import com.bank.digital.dto.response.TransferResponse;
import com.bank.digital.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transferências", description = "Endpoints para transferências financeiras")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Realizar transferência entre contas")
    @ApiResponse(responseCode = "201", description = "Transferência realizada com sucesso",
            content = @Content(schema = @Schema(implementation = TransferResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, saldo insuficiente ou transação duplicada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar transação pelo externalId")
    @ApiResponse(responseCode = "200", description = "Transação encontrada",
            content = @Content(schema = @Schema(implementation = TransferResponse.class)))
    @ApiResponse(responseCode = "404", description = "Transação não encontrada")
    @GetMapping("/{externalId}")
    public ResponseEntity<TransferResponse> getTransaction(
            @Parameter(description = "Identificador externo da transação (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable String externalId) {
        return ResponseEntity.ok(transferService.getTransactionByExternalId(externalId));
    }

    @Operation(summary = "Listar transações de uma conta")
    @ApiResponse(responseCode = "200", description = "Transações retornadas com sucesso")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransferResponse>> getTransactionsByAccount(
            @Parameter(description = "Número da conta", example = "1001")
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transferService.getTransactionsByAccount(accountNumber));
    }

    @Operation(summary = "Listar todas as transações")
    @ApiResponse(responseCode = "200", description = "Transações retornadas com sucesso")
    @GetMapping
    public ResponseEntity<List<TransferResponse>> listAllTransactions() {
        return ResponseEntity.ok(transferService.listAllTransactions());
    }
}

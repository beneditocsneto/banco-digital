package com.bank.digital.controller;

import com.bank.digital.dto.request.CreateAccountRequest;
import com.bank.digital.dto.response.AccountResponse;
import com.bank.digital.service.AccountService;
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
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Contas", description = "Endpoints para gerenciamento de contas bancárias")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Criar nova conta bancária")
    @ApiResponse(responseCode = "201", description = "Conta criada com sucesso", content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou conta já existente")
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar todas as contas")
    @ApiResponse(responseCode = "200", description = "Lista de contas retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAllAccounts() {
        return ResponseEntity.ok(accountService.listAllAccounts());
    }

    @Operation(summary = "Buscar conta por número")
    @ApiResponse(responseCode = "200", description = "Conta encontrada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @Parameter(description = "Número da conta", example = "1001")
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }
}

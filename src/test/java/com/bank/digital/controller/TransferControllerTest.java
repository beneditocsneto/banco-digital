package com.bank.digital.controller;

import com.bank.digital.dto.request.TransferRequest;
import com.bank.digital.dto.response.TransferResponse;
import com.bank.digital.exception.AccountNotFoundException;
import com.bank.digital.exception.InsufficientBalanceException;
import com.bank.digital.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @Test
    @DisplayName("POST /api/v1/transfers - Deve realizar transferência com sucesso")
    void shouldTransferSuccessfully() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1001")
                .targetAccountNumber("1002")
                .amount(new BigDecimal("500.00"))
                .currency("BRL")
                .externalId("ext-123")
                .build();

        TransferResponse response = TransferResponse.builder()
                .id(1L)
                .externalId("ext-123")
                .sourceAccountNumber("1001")
                .targetAccountNumber("1002")
                .amount(new BigDecimal("500.00"))
                .currency("BRL")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalId").value("ext-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceAccountNumber").value("1001"))
                .andExpect(jsonPath("$.targetAccountNumber").value("1002"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Deve retornar 404 quando conta não existe")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("9999")
                .targetAccountNumber("1002")
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .externalId("ext-404")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException("Conta não encontrada: 9999"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conta não encontrada: 9999"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Deve retornar 400 quando saldo é insuficiente")
    void shouldReturn400WhenInsufficientBalance() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1001")
                .targetAccountNumber("1002")
                .amount(new BigDecimal("99999.00"))
                .currency("BRL")
                .externalId("ext-400")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new InsufficientBalanceException("Saldo insuficiente"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente"));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Deve retornar 400 para dados inválidos")
    void shouldReturn400ForInvalidInput() throws Exception {
        TransferRequest invalidRequest = TransferRequest.builder()
                .sourceAccountNumber("")
                .targetAccountNumber("")
                .amount(new BigDecimal("-100.00"))
                .currency("")
                .externalId("")
                .build();

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/transfers/{externalId} - Deve buscar transação por externalId")
    void shouldGetTransactionByExternalId() throws Exception {
        TransferResponse response = TransferResponse.builder()
                .id(1L)
                .externalId("ext-999")
                .sourceAccountNumber("1001")
                .targetAccountNumber("1002")
                .amount(new BigDecimal("150.00"))
                .currency("BRL")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(transferService.getTransactionByExternalId("ext-999")).thenReturn(response);

        mockMvc.perform(get("/api/v1/transfers/ext-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("ext-999"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/transfers/account/{accountNumber} - Deve listar transações de uma conta")
    void shouldGetTransactionsByAccount() throws Exception {
        TransferResponse tx = TransferResponse.builder()
                .id(1L)
                .externalId("ext-1")
                .sourceAccountNumber("1001")
                .targetAccountNumber("1002")
                .amount(new BigDecimal("200.00"))
                .currency("BRL")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(transferService.getTransactionsByAccount("1001")).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/transfers/account/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].externalId").value("ext-1"));
    }
}

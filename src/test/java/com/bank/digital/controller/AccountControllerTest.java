package com.bank.digital.controller;

import com.bank.digital.dto.request.CreateAccountRequest;
import com.bank.digital.dto.response.AccountResponse;
import com.bank.digital.service.AccountService;
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

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("POST /api/v1/accounts - Deve criar conta com sucesso")
    void shouldCreateAccount() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .accountNumber("2001")
                .holderName("Novo Cliente")
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .accountNumber("2001")
                .holderName("Novo Cliente")
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("2001"))
                .andExpect(jsonPath("$.holderName").value("Novo Cliente"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("GET /api/v1/accounts - Deve listar todas as contas")
    void shouldListAllAccounts() throws Exception {
        AccountResponse account1 = AccountResponse.builder()
                .id(1L).accountNumber("1001").holderName("João")
                .balance(new BigDecimal("5000.00")).build();

        AccountResponse account2 = AccountResponse.builder()
                .id(2L).accountNumber("1002").holderName("Maria")
                .balance(new BigDecimal("3000.00")).build();

        when(accountService.listAllAccounts()).thenReturn(List.of(account1, account2));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountNumber").value("1001"))
                .andExpect(jsonPath("$[1].accountNumber").value("1002"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber} - Deve buscar conta por número")
    void shouldGetAccountByNumber() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L).accountNumber("1001").holderName("João Silva")
                .balance(new BigDecimal("5000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.getAccountByNumber("1001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounts/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1001"))
                .andExpect(jsonPath("$.holderName").value("João Silva"))
                .andExpect(jsonPath("$.balance").value(5000.00));
    }

    @Test
    @DisplayName("POST /api/v1/accounts - Deve retornar 400 para dados inválidos")
    void shouldReturn400ForInvalidInput() throws Exception {
        CreateAccountRequest invalidRequest = CreateAccountRequest.builder()
                .accountNumber("")
                .holderName("")
                .initialBalance(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}

package com.ahmedesawy.E_Wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ahmedesawy.E_Wallet.dto.request.DepositRequest;
import com.ahmedesawy.E_Wallet.dto.request.LoginRequest;
import com.ahmedesawy.E_Wallet.dto.request.RegisterRequest;
import com.ahmedesawy.E_Wallet.dto.request.TransferRequest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EWalletApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	StringRedisTemplate redisTemplate;

	@BeforeEach
	void clearRedis() {
		if (redisTemplate.getConnectionFactory() != null) {
			redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
		}
	}

	@Test
	@DisplayName("Security & Validation: Should return 401 without token and 400 on invalid registration DTO")
	void securityAndValidation_ShouldEnforceRules() throws Exception {
		// 1. Unauthenticated request to protected wallet endpoint
		// 401 Unauthorized
		mockMvc.perform(get("/wallets/me"))
				.andExpect(status().isUnauthorized());

		// 2. Invalid RegisterRequest (bad email + short password + 2-digit PIN)
		// 400 Bad Request
		RegisterRequest invalidRequest = new RegisterRequest(
				"s",
				"wrongemail",
				"123",
				"01012345678",
				"12");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("End-to-End Flow: Register -> Login -> Deposit -> Transfer -> History -> Logout-All -> 401")
	void fullCustomerJourney_ShouldWorkEndToEnd() throws Exception {
		// Register Sender -> Ahmed and Receiver -> Ali
		RegisterRequest ahmedReg = new RegisterRequest(
				"Ahmed",
				"ahmed@gmail.com",
				"123456",
				"0101111111111",
				"1234");
		RegisterRequest aliReg = new RegisterRequest(
				"Ali",
				"ali@gmail.com",
				"123456",
				"0102222222222",
				"5678");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ahmedReg)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.walletBalance").value(0));

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(aliReg)))
				.andExpect(status().isCreated());

		// Login As Ahmed to get JWT acessToken
		LoginRequest loginRequest = new LoginRequest(
				"ahmed@gmail.com",
				"123456");
		MvcResult loginResult = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String ahmedToken = loginJson.get("accessToken").asString();

		// Deposit 500.00 to ahmed
		DepositRequest depositRequest = new DepositRequest(new BigDecimal("500.00"));
		mockMvc.perform(post("/wallets/deposit")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ahmedToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(depositRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value("500.0"));

		// Transfer 150.00 from ahmed to ali
		TransferRequest transferRequest = new TransferRequest(
				"0102222222222", new BigDecimal("150.00"), "1234");
		mockMvc.perform(post("/wallets/transfer")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ahmedToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(transferRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.senderNewBalance").value(350.00))
				.andExpect(jsonPath("$.status").value("SUCCESS"));

		// Verify Ahmed's transaction history shows 2 records (Deposit & Transfer)
		mockMvc.perform(get("/wallets/me/transactions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ahmedToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(2));

		// Logout from all devices -> Redis revokes Ahmed's token immediately
		mockMvc.perform(post("/auth/logout-all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ahmedToken))
				.andExpect(status().isNoContent());

		// Using Ahmed's revoked token again must now fail with 401 Unauthorized
		mockMvc.perform(get("/wallets/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + ahmedToken))
				.andExpect(status().isUnauthorized());
	}
}

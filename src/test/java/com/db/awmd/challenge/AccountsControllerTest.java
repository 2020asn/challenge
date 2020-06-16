package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private static final String INSUFFICIENT_BALANCE = "Transfer process cannot be completed as account has insufficient balance.";
	private static final String SUCCESS = "Transfer was successful.";
	private static final String INVALID_AMOUNT = "Amount to transfer is not a positive number. Please validate.";

	@Before
	public void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	public void createAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		Account account = accountsService.getAccount("Id-123");
		assertThat(account.getAccountId()).isEqualTo("Id-123");
		assertThat(account.getBalance()).isEqualByComparingTo("1000");
	}

	@Test
	public void createDuplicateAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNoAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"balance\":1000}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNoBalance() throws Exception {
		this.mockMvc.perform(
				post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":\"Id-123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNoBody() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNegativeBalance() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountEmptyAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void getAccount() throws Exception {
		String uniqueAccountId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
		this.accountsService.createAccount(account);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
	}

	@Test
	public void fundsTransferSuccess() throws Exception {
		String fromID = "Id-1" + System.currentTimeMillis();
		Account fromAccount = new Account(fromID, new BigDecimal("1000"));
		this.accountsService.createAccount(fromAccount);

		String toID = "Id-2" + System.currentTimeMillis();
		Account toAccount = new Account(toID, new BigDecimal("200"));
		this.accountsService.createAccount(toAccount);

		this.mockMvc
				.perform(post("/v1/accounts/fundsTransfer").contentType(MediaType.APPLICATION_JSON).content(
						"{\"accountFrom\":\"" + fromID + "\",\"accountTo\":\"" + toID + "\",\"amount\":250.75}"))
				.andExpect(status().isOk()).andExpect(content().string(SUCCESS));
	}

	@Test
	public void fundsTransferInsufficientBalance() throws Exception {
		String fromID = "Id-3" + System.currentTimeMillis();
		Account fromAccount = new Account(fromID, new BigDecimal("1000"));
		this.accountsService.createAccount(fromAccount);

		String toID = "Id-4" + System.currentTimeMillis();
		Account toAccount = new Account(toID, new BigDecimal("200"));
		this.accountsService.createAccount(toAccount);

		this.mockMvc
				.perform(post("/v1/accounts/fundsTransfer").contentType(MediaType.APPLICATION_JSON).content(
						"{\"accountFrom\":\"" + fromID + "\",\"accountTo\":\"" + toID + "\",\"amount\":1250.75}"))
				.andExpect(status().isOk()).andExpect(content().string(INSUFFICIENT_BALANCE));
	}

	@Test
	public void fundsTransferInvalidAmount() throws Exception {
		String fromID = "Id-5" + System.currentTimeMillis();
		Account fromAccount = new Account(fromID, new BigDecimal("1000"));
		this.accountsService.createAccount(fromAccount);

		String toID = "Id-6" + System.currentTimeMillis();
		Account toAccount = new Account(toID, new BigDecimal("200"));
		this.accountsService.createAccount(toAccount);

		this.mockMvc
				.perform(post("/v1/accounts/fundsTransfer").contentType(MediaType.APPLICATION_JSON).content(
						"{\"accountFrom\":\"" + fromID + "\",\"accountTo\":\"" + toID + "\",\"amount\":-250.75}"))
				.andExpect(status().isOk()).andExpect(content().string(INVALID_AMOUNT));
	}
}

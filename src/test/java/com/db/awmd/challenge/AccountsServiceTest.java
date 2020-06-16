package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.FundsTransfer;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	private static final String INSUFFICIENT_BALANCE = "Transfer process cannot be completed as account has insufficient balance.";
	private static final String SUCCESS = "Transfer was successful.";
	private static final String INVALID_AMOUNT = "Amount to transfer is not a positive number. Please validate.";

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}

	}

	@Test
	public void fundsTransfer_First() {
		String debitAcc = "Id-12345";
		String creditAcc = "Id-34567";
		createTestData(debitAcc, creditAcc);

		FundsTransfer request = FundsTransfer.builder().accountFrom(debitAcc).accountTo(creditAcc)
				.amount(new BigDecimal("400.25")).build();
		String message = this.accountsService.fundsTransfer(request);
		assertSame(SUCCESS, message);
	}

	public void createTestData(String debitAcc, String creditAcc) {
		if (this.accountsService.getAccount(debitAcc) == null) {
			Account accountFrom = new Account(debitAcc);
			accountFrom.setBalance(new BigDecimal(1000));
			this.accountsService.createAccount(accountFrom);
		}

		if (this.accountsService.getAccount(creditAcc) == null) {
			Account accountTo = new Account(creditAcc);
			accountTo.setBalance(new BigDecimal(1000));
			this.accountsService.createAccount(accountTo);
		}
	}

	@Test
	public void fundsTransfer_Second() {
		String debitAcc = "Id-34567";
		String creditAcc = "Id-12345";
		createTestData(debitAcc, creditAcc);

		FundsTransfer request = FundsTransfer.builder().accountFrom(debitAcc).accountTo(creditAcc)
				.amount(new BigDecimal("200.25")).build();
		String message = this.accountsService.fundsTransfer(request);
		assertSame(SUCCESS, message);
	}

	@Test
	public void fundsTransfer_InvalidAmount() {

		Account accountFrom = new Account("Id-123456");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);

		Account accountTo = new Account("Id-345678");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);

		FundsTransfer request = FundsTransfer.builder().accountFrom(accountFrom.getAccountId())
				.accountTo(accountTo.getAccountId()).amount(new BigDecimal("-400")).build();
		String message = this.accountsService.fundsTransfer(request);
		assertSame(INVALID_AMOUNT, message);
	}

	@Test
	public void fundsTransfer_InsufficientBalance() {
		Account accountFrom = new Account("Id-1244");
		accountFrom.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountFrom);

		Account accountTo = new Account("Id-3455");
		accountTo.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(accountTo);

		FundsTransfer request = FundsTransfer.builder().accountFrom(accountFrom.getAccountId())
				.accountTo(accountTo.getAccountId()).amount(new BigDecimal("4000")).build();
		String message = this.accountsService.fundsTransfer(request);
		assertSame(INSUFFICIENT_BALANCE, message);
	}
}
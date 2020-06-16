package com.db.awmd.challenge.repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.FundsTransfer;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.service.EmailNotificationService;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class AccountsRepositoryInMemory implements AccountsRepository {

	private final Map<String, Account> accounts = new ConcurrentHashMap<>();
	private final EmailNotificationService notificationService = new EmailNotificationService();
	private final Object lock = new Object();

	private static final String INSUFFICIENT_BALANCE = "Transfer process cannot be completed as account has insufficient balance.";
	private static final String SUCCESS = "Transfer was successful.";
	private static final String INVALID_AMOUNT = "Amount to transfer is not a positive number. Please validate.";

	@Override
	public void createAccount(Account account) {
		Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
		if (previousAccount != null) {
			throw new DuplicateAccountIdException("Account id " + account.getAccountId() + " already exists!");
		}
	}

	@Override
	public Account getAccount(String accountId) {
		return accounts.get(accountId);
	}

	@Override
	public void clearAccounts() {
		accounts.clear();
	}

	@Override
	public String fundsTransfer(FundsTransfer request) {

		BigDecimal amount = BigDecimal.valueOf(request.getAmount().doubleValue());

		if (amount.compareTo(BigDecimal.ZERO) > 0) {
			Account debitAcc = getAccount(request.getAccountFrom());
			Account creditAcc = getAccount(request.getAccountTo());

			if (debitAcc.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) > 0) {
				synchronized (lock) {

					log.info("Credit Acc - Balance before transfer is: " + creditAcc.getBalance());
					creditAcc.setBalance(creditAcc.getBalance().add(amount));
					log.info("Credit Acc - Balance after transfer is: " + creditAcc.getBalance());

					log.info("Debit Acc - Balance before transfer is: " + debitAcc.getBalance());
					debitAcc.setBalance(debitAcc.getBalance().subtract(amount));
					log.info("Debit Acc - Balance after transfer is: " + debitAcc.getBalance());

					// update accounts
					accounts.put(creditAcc.getAccountId(), creditAcc);
					accounts.put(debitAcc.getAccountId(), debitAcc);

					notificationService.notifyAboutTransfer(creditAcc, amount + " has been transferred from account "
							+ debitAcc.getAccountId() + ", into your account.");
					notificationService.notifyAboutTransfer(debitAcc, amount
							+ " has been transferred from your account to " + creditAcc.getAccountId() + " account.");
				}
			} else {
				return INSUFFICIENT_BALANCE;
			}
		} else {
			// Assumption is that we are not supposed to proceed if amount is a negative number.
			// Other option/solution would be to proceed by only considering the absolute amount value.
			return INVALID_AMOUNT;
		}

		return SUCCESS;
	}
}
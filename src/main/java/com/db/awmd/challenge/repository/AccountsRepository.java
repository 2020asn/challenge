package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.FundsTransfer;

public interface AccountsRepository {

  void createAccount(Account account);

  Account getAccount(String accountId);

  void clearAccounts();
  
  String fundsTransfer(FundsTransfer request);
}

package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.exception.TransferSameAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) throws AccountNotFoundException {
    return this.accountsRepository.getAccount(accountId).orElseThrow(
      () -> new AccountNotFoundException("Account " + accountId + " not found!"));
  }

  public void transfer(Transfer transfer) throws
    AccountNotFoundException,
    TransferSameAccountException,
    NotEnoughFundsException {
    if (accountsAreEqual(transfer.getFromAccountId(), transfer.getToAccountId())) {
      throw new TransferSameAccountException(
        "Transfer to the same account (Id: )" + transfer.getFromAccountId() + " is not allowed!"
      );
    }

    Pair<Account, Account> transferAccounts = this.syncTransferOperations(transfer);

    notificationService.notifyAboutTransfer(
      transferAccounts.getKey(),
      "Transfer to account " + transfer.getToAccountId() + " for amount " + transfer.getAmount()
    );

    notificationService.notifyAboutTransfer(
      transferAccounts.getValue(),
      "Transfer from account " + transfer.getFromAccountId() + " for amount " + transfer.getAmount()
    );
  }

  @Synchronized
  private Pair<Account, Account> syncTransferOperations(Transfer transfer) throws
    AccountNotFoundException,
    NotEnoughFundsException
  {
    Account fromAccount = this.getAccount(transfer.getFromAccountId());
    Account toAccount = this.getAccount(transfer.getToAccountId());

    if (!accountHasEnoughMoney(fromAccount, transfer.getAmount())) {
      throw new NotEnoughFundsException(
              "Account " + fromAccount.getAccountId() + " does not have enough money!"
      );
    }

    fromAccount.setBalance(fromAccount.getBalance().subtract(transfer.getAmount()));
    toAccount.setBalance(toAccount.getBalance().add(transfer.getAmount()));

    this.accountsRepository.updateAccount(fromAccount);
    this.accountsRepository.updateAccount(toAccount);

    return new Pair<>(fromAccount, toAccount);
  }

  private boolean accountHasEnoughMoney(Account account, BigDecimal amount) {
    return account.getBalance().compareTo(amount) >= 0;
}

  private boolean accountsAreEqual(String fromAccount, String toAccount) {
    return !fromAccount.equals(toAccount);
  }
}

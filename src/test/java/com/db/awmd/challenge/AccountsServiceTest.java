package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.*;
import com.db.awmd.challenge.service.AccountsService;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.db.awmd.challenge.service.NotificationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @MockBean
  private NotificationService notificationService;

  private Account account1;
  private Account account2;

  @Before
  public void setUp() {
    account1 = new Account("Id-1", new BigDecimal(500));
    account2 = new Account("Id-2", new BigDecimal(0));
    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);
  }

  @After
  public void tearDown() {
    this.accountsService.getAccountsRepository().clearAccounts();
  }

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
  public void transfer() {
    this.accountsService.transfer(
      new Transfer(account1.getAccountId(), account2.getAccountId(), new BigDecimal(100))
    );

    assertThat(account1.getBalance()).isEqualTo(new BigDecimal(400));
    assertThat(account2.getBalance()).isEqualTo(new BigDecimal(100));

    verify(notificationService, times(1))
      .notifyAboutTransfer(account1, "Transfer to account " + account2.getAccountId() + " for amount 100");
    verify(notificationService, times(1))
      .notifyAboutTransfer(account2, "Transfer from account " + account1.getAccountId() + " for amount 100");
  }

  @Test(expected = TransferSameAccountException.class)
  public void transfer_failsOnSameAccount() {
    this.accountsService.transfer(
      new Transfer(account1.getAccountId(), account1.getAccountId(), new BigDecimal(100))
    );
  }

  @Test(expected = NotEnoughFundsException.class)
  public void transfer_failsOnNotEnoughFunds() {
    this.accountsService.transfer(
      new Transfer(account2.getAccountId(), account1.getAccountId(), new BigDecimal(6))
    );
  }

  @Test(expected = AccountNotFoundException.class)
  public void transfer_failsOnAccountNotFound() {
    this.accountsService.transfer(
      new Transfer("not-existing", account1.getAccountId(), new BigDecimal(6))
    );
  }

  @Test(expected = TransferNoAmountException.class)
  public void transfer_failsOnNoAmount() {
    this.accountsService.transfer(
      new Transfer(account1.getAccountId(), account2.getAccountId(), new BigDecimal(0))
    );
  }

  @Test(expected = TransferNoAmountException.class)
  public void transfer_failsOnNegativeAmount() {
    this.accountsService.transfer(
      new Transfer(account1.getAccountId(), account2.getAccountId(), new BigDecimal(-1))
    );
  }

  @Test
  public void transfer_concorrentTransfer() throws InterruptedException {
    // transfer 25 times 5 from account1 to account2
    // transfer 25 times 1 from account2 to account1
    makeTransferThreads(50);

    assertThat(account1.getBalance()).isEqualTo(new BigDecimal(400));
    assertThat(account2.getBalance()).isEqualTo(new BigDecimal(100));

    verify(notificationService, times(25))
      .notifyAboutTransfer(account1, "Transfer to account " + account2.getAccountId() + " for amount 5");
    verify(notificationService, times(25))
      .notifyAboutTransfer(account2, "Transfer from account " + account1.getAccountId() + " for amount 5");
    verify(notificationService, times(25))
      .notifyAboutTransfer(account2, "Transfer to account " + account1.getAccountId() + " for amount 1");
    verify(notificationService, times(25))
      .notifyAboutTransfer(account1, "Transfer from account " + account2.getAccountId() + " for amount 1");
  }

  private void makeTransferThreads(int numberOfThreads) throws InterruptedException {
    ExecutorService service = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      int finalI = i;
      service.submit(() -> {
        if (finalI % 2 == 0) {
          accountsService.transfer(
            new Transfer(account1.getAccountId(), account2.getAccountId(), new BigDecimal(5))
          );
        } else {
          accountsService.transfer(
            new Transfer(account2.getAccountId(), account1.getAccountId(), new BigDecimal(1))
          );
        }

        latch.countDown();
      });
    }

    latch.await();
  }

}

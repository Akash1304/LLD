package atm.service;

import atm.model.Account;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBankService implements BankService {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Account openAccount(String accountId, double initialBalance) {
        Account account = new Account(accountId, initialBalance);
        accounts.put(accountId, account);
        return account;
    }

    @Override
    public double getBalance(String accountId) {
        return requireAccount(accountId).getBalance();
    }

    // Delegates to Account.tryWithdraw's atomic check-and-debit instead of
    // synchronizing this whole service: withdrawing from one account
    // should never block a concurrent withdrawal from a different account.
    @Override
    public void withdraw(String accountId, double amount) throws InsufficientFundsException {
        Account account = requireAccount(accountId);
        if (!account.tryWithdraw(amount)) {
            throw new InsufficientFundsException("Balance $" + account.getBalance() + " is less than requested $" + amount);
        }
    }

    @Override
    public void deposit(String accountId, double amount) {
        requireAccount(accountId).deposit(amount);
    }

    private Account requireAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) throw new IllegalArgumentException("Unknown account: " + accountId);
        return account;
    }
}

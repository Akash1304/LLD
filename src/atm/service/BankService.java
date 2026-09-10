package atm.service;

import atm.model.Account;

public interface BankService {
    Account openAccount(String accountId, double initialBalance);
    double getBalance(String accountId);
    void withdraw(String accountId, double amount) throws InsufficientFundsException;
    void deposit(String accountId, double amount);

    class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) { super(message); }
    }
}

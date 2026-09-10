package atm.state;

import atm.service.ATMMachine;
import atm.service.BankService;

import java.util.Map;

public class AuthenticatedState implements ATMState {
    @Override
    public void selectWithdrawal(ATMMachine atm, double amount) {
        String accountId = atm.getCurrentCard().getAccountId();
        BankService bank = atm.getBankService();
        try {
            bank.withdraw(accountId, amount);
        } catch (BankService.InsufficientFundsException e) {
            System.out.println("  Withdrawal denied: " + e.getMessage());
            return;
        }

        try {
            Map<Integer, Integer> bills = atm.getCashDispenser().dispense((int) amount);
            System.out.println("  Dispensing $" + amount + " as: " + bills);
        } catch (RuntimeException e) {
            // roll back the debit since the machine couldn't physically dispense the cash
            bank.deposit(accountId, amount);
            System.out.println("  Cash dispenser could not fulfill the request (" + e.getMessage() + "); withdrawal reversed.");
            return;
        }

        System.out.println("  New balance: $" + bank.getBalance(accountId));
    }

    @Override
    public void selectBalanceInquiry(ATMMachine atm) {
        double balance = atm.getBankService().getBalance(atm.getCurrentCard().getAccountId());
        System.out.println("  Current balance: $" + balance);
    }

    @Override
    public void ejectCard(ATMMachine atm) {
        System.out.println("  Transaction complete. Card ejected.");
        atm.setCurrentCard(null);
        atm.setState(atm.getIdleState());
    }

    @Override
    public String getName() { return "AUTHENTICATED"; }
}

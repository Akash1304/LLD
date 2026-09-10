package atm.model;

public class Account {
    private final String id;
    private double balance;

    public Account(String id, double balance) {
        this.id = id;
        this.balance = balance;
    }

    public String getId() { return id; }
    public synchronized double getBalance() { return balance; }

    // Atomic "withdraw if sufficient funds" -- one monitor per account, so
    // withdrawals against DIFFERENT accounts never contend, and two
    // concurrent withdrawals against the SAME account (e.g. a card used at
    // two ATMs at once) can't both pass the balance check and overdraw it.
    public synchronized boolean tryWithdraw(double amount) {
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }

    public synchronized void deposit(double amount) {
        balance += amount;
    }
}

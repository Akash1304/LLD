package atm.interview;

import java.util.*;

// Compact, single-file interview-friendly ATM demo.
// Supports: the same State-pattern flow as the full version (idle -> has
// card -> authenticated), collapsed into one file with a switch-based
// state instead of separate state classes.
public class SimpleATMInterview {
    enum State { IDLE, HAS_CARD, AUTHENTICATED }

    State state = State.IDLE;
    int correctPin = 4321;
    int failedAttempts = 0;
    double balance = 500.0;
    final Map<Integer, Integer> cash = new TreeMap<>(Collections.reverseOrder());

    SimpleATMInterview() {
        cash.put(100, 5);
        cash.put(20, 10);
    }

    void insertCard() {
        if (state != State.IDLE) { reject("insert card"); return; }
        state = State.HAS_CARD;
        System.out.println("  Card inserted. Enter PIN.");
    }

    void enterPin(int pin) {
        if (state != State.HAS_CARD) { reject("enter PIN"); return; }
        if (pin == correctPin) {
            state = State.AUTHENTICATED;
            failedAttempts = 0;
            System.out.println("  PIN correct.");
        } else {
            failedAttempts++;
            if (failedAttempts >= 3) {
                System.out.println("  Too many wrong attempts, ejecting card.");
                state = State.IDLE;
            } else {
                System.out.println("  Wrong PIN, attempts left: " + (3 - failedAttempts));
            }
        }
    }

    void withdraw(int amount) {
        if (state != State.AUTHENTICATED) { reject("withdraw"); return; }
        if (amount > balance) { System.out.println("  Insufficient funds."); return; }
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        int remaining = amount;
        for (Map.Entry<Integer, Integer> e : cash.entrySet()) {
            int need = Math.min(e.getValue(), remaining / e.getKey());
            if (need > 0) { plan.put(e.getKey(), need); remaining -= need * e.getKey(); }
        }
        if (remaining != 0) { System.out.println("  Cannot dispense that amount with available bills."); return; }
        plan.forEach((denom, count) -> cash.merge(denom, -count, Integer::sum));
        balance -= amount;
        System.out.println("  Dispensed: " + plan + ", new balance: $" + balance);
    }

    void ejectCard() {
        if (state == State.IDLE) { reject("eject card"); return; }
        state = State.IDLE;
        System.out.println("  Card ejected.");
    }

    void reject(String op) { System.out.println("  [" + state + "] Cannot " + op + " right now."); }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleATMInterview atm = new SimpleATMInterview();
        System.out.println("== Simple ATM Interview Demo ==");

        System.out.println("Trying to enter a PIN before inserting a card:");
        atm.enterPin(4321);

        System.out.println("\nInserting card, wrong PIN then correct PIN:");
        atm.insertCard();
        atm.enterPin(1111);
        atm.enterPin(4321);

        System.out.println("\nWithdrawing $140:");
        atm.withdraw(140);

        System.out.println("\nTrying to withdraw $1000 (exceeds balance):");
        atm.withdraw(1000);

        System.out.println("\nEjecting card:");
        atm.ejectCard();

        System.out.println("\nTrying to withdraw after eject:");
        atm.withdraw(20);
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}

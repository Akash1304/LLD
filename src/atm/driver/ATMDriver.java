package atm.driver;

import atm.model.Card;
import atm.model.CashDispenser;
import atm.service.ATMMachine;
import atm.service.BankService;
import atm.service.InMemoryBankService;

public class ATMDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        BankService bank = new InMemoryBankService();
        bank.openAccount("ACC1", 500.0);

        CashDispenser dispenser = new CashDispenser();
        dispenser.loadCash(100, 5);
        dispenser.loadCash(20, 10);

        ATMMachine atm = new ATMMachine(bank, dispenser);
        Card card = new Card("CARD-1", "ACC1", 4321);

        System.out.println("== ATM Demo ==");
        System.out.println("State: " + atm.getCurrentStateName());

        System.out.println("\nTrying to enter a PIN before inserting a card (invalid in IDLE state):");
        atm.enterPin(4321);

        System.out.println("\nInserting card:");
        atm.insertCard(card);
        System.out.println("State: " + atm.getCurrentStateName());

        System.out.println("\nEntering wrong PIN twice, then the correct one:");
        atm.enterPin(1111);
        atm.enterPin(2222);
        atm.enterPin(4321);
        System.out.println("State: " + atm.getCurrentStateName());

        System.out.println("\nChecking balance:");
        atm.selectBalanceInquiry();

        System.out.println("\nWithdrawing $140 (should dispense one $100 + two $20 bills):");
        atm.selectWithdrawal(140);

        System.out.println("\nTrying to withdraw $1000 (exceeds balance):");
        atm.selectWithdrawal(1000);

        System.out.println("\nEjecting card:");
        atm.ejectCard();
        System.out.println("State: " + atm.getCurrentStateName());

        System.out.println("\nTrying to withdraw after the card was ejected (invalid in IDLE state):");
        atm.selectWithdrawal(20);

        System.out.println("\nSecond card holder locks their card out after 3 wrong PINs:");
        Card card2 = new Card("CARD-2", "ACC1", 9999);
        atm.insertCard(card2);
        atm.enterPin(1111);
        atm.enterPin(2222);
        atm.enterPin(3333);
        System.out.println("State: " + atm.getCurrentStateName());
    }
}

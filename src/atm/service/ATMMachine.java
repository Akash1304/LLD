package atm.service;

import atm.model.Card;
import atm.model.CashDispenser;
import atm.state.ATMState;
import atm.state.AuthenticatedState;
import atm.state.HasCardState;
import atm.state.IdleState;

// The State-pattern "context": holds the current state and the session
// data (inserted card, failed PIN attempts), and delegates every operation
// to whichever ATMState is currently active. Callers (the driver, a real
// UI) only ever call methods on ATMMachine -- they never touch ATMState
// directly or check "what state am I in" themselves.
public class ATMMachine {
    public static final int MAX_PIN_ATTEMPTS = 3;

    private final BankService bankService;
    private final CashDispenser cashDispenser;

    private final ATMState idleState = new IdleState();
    private final ATMState hasCardState = new HasCardState();
    private final ATMState authenticatedState = new AuthenticatedState();

    private ATMState currentState = idleState;
    private Card currentCard;
    private int failedPinAttempts = 0;

    public ATMMachine(BankService bankService, CashDispenser cashDispenser) {
        this.bankService = bankService;
        this.cashDispenser = cashDispenser;
    }

    // Every entry point is synchronized on this machine instance -- one
    // physical ATM has one keypad and serves one session at a time, but
    // making that explicit (rather than relying on "well, nothing calls
    // it concurrently in practice") protects against a hypothetical
    // multi-channel front-end (e.g. a web session and the physical
    // keypad both driving the same account) and against a state method
    // (which itself calls back into these accessors) racing a fresh
    // top-level call. Java monitors are reentrant, so a state's internal
    // calls back into the synchronized accessors below compose safely
    // with the outer lock already held by the entry point.
    public synchronized void insertCard(Card card) { currentState.insertCard(this, card); }
    public synchronized void enterPin(int pin) { currentState.enterPin(this, pin); }
    public synchronized void selectWithdrawal(double amount) { currentState.selectWithdrawal(this, amount); }
    public synchronized void selectBalanceInquiry() { currentState.selectBalanceInquiry(this); }
    public synchronized void ejectCard() { currentState.ejectCard(this); }

    public synchronized void rejectOperation(String operation) {
        System.out.println("  [" + currentState.getName() + "] Cannot " + operation + " right now.");
    }

    public synchronized void setState(ATMState state) { this.currentState = state; }
    public ATMState getIdleState() { return idleState; }
    public ATMState getHasCardState() { return hasCardState; }
    public ATMState getAuthenticatedState() { return authenticatedState; }

    public BankService getBankService() { return bankService; }
    public CashDispenser getCashDispenser() { return cashDispenser; }

    public synchronized Card getCurrentCard() { return currentCard; }
    public synchronized void setCurrentCard(Card card) { this.currentCard = card; }

    public synchronized int getFailedPinAttempts() { return failedPinAttempts; }
    public synchronized void incrementFailedPinAttempts() { failedPinAttempts++; }
    public synchronized void resetFailedPinAttempts() { failedPinAttempts = 0; }

    public synchronized String getCurrentStateName() { return currentState.getName(); }
}

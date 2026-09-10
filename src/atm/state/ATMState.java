package atm.state;

import atm.model.Card;
import atm.service.ATMMachine;

// The State-pattern contract: each concrete state overrides only the
// transitions that are valid from it. The default methods mean a state
// doesn't have to explicitly reject every operation it doesn't support --
// invalid operations for a given state just fall through to a shared
// "not allowed right now" message on the machine.
public interface ATMState {
    default void insertCard(ATMMachine atm, Card card) { atm.rejectOperation("insert card"); }
    default void enterPin(ATMMachine atm, int pin) { atm.rejectOperation("enter PIN"); }
    default void selectWithdrawal(ATMMachine atm, double amount) { atm.rejectOperation("withdraw cash"); }
    default void selectBalanceInquiry(ATMMachine atm) { atm.rejectOperation("check balance"); }
    default void ejectCard(ATMMachine atm) { atm.rejectOperation("eject card"); }

    String getName();
}

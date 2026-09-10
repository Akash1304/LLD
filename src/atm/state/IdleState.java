package atm.state;

import atm.model.Card;
import atm.service.ATMMachine;

public class IdleState implements ATMState {
    @Override
    public void insertCard(ATMMachine atm, Card card) {
        atm.setCurrentCard(card);
        atm.resetFailedPinAttempts();
        atm.setState(atm.getHasCardState());
        System.out.println("  Card inserted. Please enter your PIN.");
    }

    @Override
    public String getName() { return "IDLE"; }
}

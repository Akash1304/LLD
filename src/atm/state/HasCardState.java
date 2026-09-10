package atm.state;

import atm.service.ATMMachine;

public class HasCardState implements ATMState {
    @Override
    public void enterPin(ATMMachine atm, int pin) {
        if (atm.getCurrentCard().isPinCorrect(pin)) {
            atm.resetFailedPinAttempts();
            atm.setState(atm.getAuthenticatedState());
            System.out.println("  PIN correct. Select a transaction.");
            return;
        }

        atm.incrementFailedPinAttempts();
        if (atm.getFailedPinAttempts() >= ATMMachine.MAX_PIN_ATTEMPTS) {
            System.out.println("  Incorrect PIN. Maximum attempts reached -- ejecting card.");
            atm.setCurrentCard(null);
            atm.setState(atm.getIdleState());
        } else {
            System.out.println("  Incorrect PIN. Attempts remaining: " + (ATMMachine.MAX_PIN_ATTEMPTS - atm.getFailedPinAttempts()));
        }
    }

    @Override
    public void ejectCard(ATMMachine atm) {
        System.out.println("  Card ejected.");
        atm.setCurrentCard(null);
        atm.setState(atm.getIdleState());
    }

    @Override
    public String getName() { return "HAS_CARD"; }
}

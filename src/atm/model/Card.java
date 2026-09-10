package atm.model;

public class Card {
    private final String cardNumber;
    private final String accountId;
    private final int pin;

    public Card(String cardNumber, String accountId, int pin) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
        this.pin = pin;
    }

    public String getCardNumber() { return cardNumber; }
    public String getAccountId() { return accountId; }
    public boolean isPinCorrect(int enteredPin) { return pin == enteredPin; }
}

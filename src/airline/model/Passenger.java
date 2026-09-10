package airline.model;

import java.util.Objects;

public class Passenger {
    private final String passportNumber;
    private final String name;

    public Passenger(String passportNumber, String name) {
        this.passportNumber = passportNumber;
        this.name = name;
    }

    public String getPassportNumber() { return passportNumber; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Passenger)) return false;
        return Objects.equals(passportNumber, ((Passenger) o).passportNumber);
    }

    @Override
    public int hashCode() { return Objects.hash(passportNumber); }

    @Override
    public String toString() { return name + " (" + passportNumber + ")"; }
}

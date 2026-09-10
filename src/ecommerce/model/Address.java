package ecommerce.model;

public class Address {
    private final String street;
    private final String city;
    private final String zip;

    public Address(String street, String city, String zip) {
        this.street = street;
        this.city = city;
        this.zip = zip;
    }

    @Override
    public String toString() { return street + ", " + city + " " + zip; }
}

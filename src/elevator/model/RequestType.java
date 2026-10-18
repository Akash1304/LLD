package elevator.model;

// A hall call carries the direction the caller wants to travel; a
// destination (pressed inside the car) has no direction. Keeping the type
// on the request is what lets the elevator skip a PICKUP_DOWN while it is
// sweeping up, instead of picking up someone who wants to go the other way.
public enum RequestType {
    PICKUP_UP, PICKUP_DOWN, DESTINATION
}

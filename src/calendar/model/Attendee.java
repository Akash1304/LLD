package calendar.model;

public class Attendee {
    public enum ResponseStatus { INVITED, ACCEPTED, DECLINED }

    private final User user;
    private ResponseStatus status;

    public Attendee(User user, ResponseStatus status) {
        this.user = user;
        this.status = status;
    }

    public User getUser() { return user; }
    public ResponseStatus getStatus() { return status; }
    public void setStatus(ResponseStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Attendee{" + "user=" + user + ", status=" + status + '}';
    }
}


package notification.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    // channels tried in order until one succeeds -- e.g. try a push
    // notification first, fall back to email, then SMS as a last resort
    private final List<NotificationChannelType> channelPreferenceOrder;

    public User(String id, String name, String email, String phone, List<NotificationChannelType> channelPreferenceOrder) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.channelPreferenceOrder = new ArrayList<>(channelPreferenceOrder);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public List<NotificationChannelType> getChannelPreferenceOrder() { return channelPreferenceOrder; }
}

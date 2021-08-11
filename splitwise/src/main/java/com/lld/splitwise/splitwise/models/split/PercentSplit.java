package com.lld.splitwise.splitwise.models.split;

import com.lld.splitwise.splitwise.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PercentSplit extends Split {
    private double percentage;

    public PercentSplit(User user, double percentage) {
        super(user);
        this.percentage = percentage;
    }
}

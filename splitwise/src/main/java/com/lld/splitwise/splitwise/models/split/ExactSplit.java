package com.lld.splitwise.splitwise.models.split;

import com.lld.splitwise.splitwise.models.User;

public class ExactSplit extends Split {
    public ExactSplit(User user, double amount) {
        super(user);
        this.amount = amount;
    }
}

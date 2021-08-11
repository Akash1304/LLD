package com.lld.splitwise.splitwise.models.split;

import com.lld.splitwise.splitwise.models.User;
import lombok.Data;

@Data
public abstract class Split {
    private User user;
    double amount;

    public Split(User user){
        this.user = user;
    }
}

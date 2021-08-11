package com.lld.splitwise.splitwise.models;

import com.lld.splitwise.splitwise.models.ExpenseMetadata;
import com.lld.splitwise.splitwise.models.User;
import com.lld.splitwise.splitwise.models.split.Split;
import lombok.Data;

import java.util.List;

@Data
public abstract class Expense {
    private String id;
    private double amount;
    private User paidBy;
    private List<Split> splits;
    private ExpenseMetadata expenseMetadata;

    public Expense(double amount, User paidBy, List<Split> splits, ExpenseMetadata expenseMetaData) {
        this.amount = amount;
        this.paidBy = paidBy;
        this.splits = splits;
        this.expenseMetadata = expenseMetaData;
    }

    public abstract boolean validate();
}

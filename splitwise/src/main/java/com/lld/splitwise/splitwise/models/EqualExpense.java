package com.lld.splitwise.splitwise.models;

import com.lld.splitwise.splitwise.models.split.EqualSplit;
import com.lld.splitwise.splitwise.models.split.Split;

import java.util.List;

public class EqualExpense extends Expense {
    public EqualExpense(double amount, User paidBy, List<Split> splits, ExpenseMetadata expenseMetaData) {
        super(amount, paidBy, splits, expenseMetaData);
    }

    @Override
    public boolean validate() {
        for(Split split: getSplits()){
            if(!(split instanceof EqualSplit)) return false;
        }
        return false;
    }
}

package com.lld.splitwise.splitwise.models;

import com.lld.splitwise.splitwise.models.Expense;
import com.lld.splitwise.splitwise.models.User;
import com.lld.splitwise.splitwise.models.split.ExactSplit;
import com.lld.splitwise.splitwise.models.split.PercentSplit;
import com.lld.splitwise.splitwise.models.split.Split;

import java.util.List;

public class PercentageExpense extends Expense {
    public PercentageExpense(double amount, User paidBy, List<Split> splits, ExpenseMetadata expenseMetaData) {
        super(amount, paidBy, splits, expenseMetaData);
    }

    @Override
    public boolean validate() {
        for(Split split: getSplits()){
            if(!(split instanceof PercentSplit)) return false;
        }

        double totalPercent = 100;
        double sumSplitPercent = 0;

        for(Split split: getSplits()){

            PercentSplit percentSplit = (PercentSplit) split;
            sumSplitPercent+=percentSplit.getPercentage();
        }

        if(sumSplitPercent!=totalPercent) return false;

        return true;
    }
}

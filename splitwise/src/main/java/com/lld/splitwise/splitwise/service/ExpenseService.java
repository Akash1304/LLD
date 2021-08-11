package com.lld.splitwise.splitwise.service;

import com.lld.splitwise.splitwise.enums.ExpenseType;
import com.lld.splitwise.splitwise.models.EqualExpense;
import com.lld.splitwise.splitwise.models.ExactExpense;
import com.lld.splitwise.splitwise.models.Expense;
import com.lld.splitwise.splitwise.models.ExpenseMetadata;
import com.lld.splitwise.splitwise.models.PercentageExpense;
import com.lld.splitwise.splitwise.models.User;
import com.lld.splitwise.splitwise.models.split.PercentSplit;
import com.lld.splitwise.splitwise.models.split.Split;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {
    public Expense createExpense(ExpenseType expenseType, double amount, User paidBy, List<Split> splits, ExpenseMetadata expenseMetadata){
        switch (expenseType){
            case EXACT:
                return new ExactExpense(amount, paidBy, splits, expenseMetadata);
            case PERCENTAGE:
                for (Split split : splits) {
                    PercentSplit percentSplit = (PercentSplit) split;
                    split.setAmount((amount*percentSplit.getPercentage())/100.0);
                }
                return new PercentageExpense(amount, paidBy, splits, expenseMetadata);
            case EQUAL:
                int totalSplits = splits.size();
                double splitAmount = ((double) Math.round(amount*100/totalSplits))/100.0;
                for (Split split : splits) {
                    split.setAmount(splitAmount);
                }
                splits.get(0).setAmount(splitAmount + (amount - splitAmount*totalSplits));
                return new EqualExpense(amount, paidBy, splits, expenseMetadata);
            default:
                return null;
        }
    }
}

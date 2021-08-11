package com.lld.splitwise.splitwise.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExpenseMetadata {
    private String name;
    private String imgUrl;
    private String notes;
}

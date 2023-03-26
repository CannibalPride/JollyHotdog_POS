package com.jolly_hotdogs.jollyhotdog;

import java.time.LocalDateTime;

public class Sales {
    private String branch;
    private String name;
    private int quantity;
    private ItemType type;
    private double price;
    private LocalDateTime transactionDate;

    public Sales(String branch, String name, int quantity, ItemType type, double price, LocalDateTime transactionDate) {
        this.branch = branch;
        this.name = name;
        this.type = type;
        this.price = price;
        this.transactionDate = transactionDate;
        this.quantity = quantity;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public ItemType getType() {
        return type;
    }
}

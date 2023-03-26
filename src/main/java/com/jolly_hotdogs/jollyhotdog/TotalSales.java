package com.jolly_hotdogs.jollyhotdog;

public class TotalSales {
    private double gross;
    private double tax;
    private double afterTax;

    public TotalSales(double gross, double tax, double afterTax) {
        this.gross = gross;
        this.tax = tax;
        this.afterTax = afterTax;
    }

    public double getGross() {
        return gross;
    }

    public void setGross(double gross) {
        this.gross = gross;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getAfterTax() {
        return afterTax;
    }

    public void setAfterTax(double afterTax) {
        this.afterTax = afterTax;
    }
}

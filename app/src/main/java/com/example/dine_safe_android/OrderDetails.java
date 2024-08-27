package com.example.dine_safe_android;

public class OrderDetails {
    public String no_of_customers;
    public String served_by;
    public String status;
    public int table_no;
    public double total_price;

    public OrderDetails(String no_of_customers, String served_by, String status, int table_no, double total_price) {
        this.no_of_customers = no_of_customers;
        this.served_by = served_by;
        this.status = status;
        this.table_no = table_no;
        this.total_price = total_price;
    }
}
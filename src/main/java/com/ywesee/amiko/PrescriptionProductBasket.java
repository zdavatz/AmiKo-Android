package com.ywesee.amiko;


import java.util.ArrayList;

public class PrescriptionProductBasket {
    static PrescriptionProductBasket shared = null;
    public ArrayList<Product> products;

    static PrescriptionProductBasket getShared() {
        if (shared == null) {
            shared = new PrescriptionProductBasket();
        }
        return shared;
    }

    private PrescriptionProductBasket() {
        this.products = new ArrayList<Product>();
    }
}

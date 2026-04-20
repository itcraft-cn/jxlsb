package cn.itcraft.jxlsb.example;

import cn.itcraft.jxlsb.api.CellData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Helly Guo
 * <p>
 * Created on 2026-04-20 09:46
 */
class Product {
    private final String name;
    private final String description;
    private final double price;
    private final int quantity;
    private final long createTime;

    public Product() {
        this.name = "Product " + (int) (Math.random() * 1000000);
        this.description = "Description " + (int) (Math.random() * 1000000);
        this.price = Math.random() * 100;
        this.quantity = (int) (Math.random() * 100);
        this.createTime = System.currentTimeMillis();
    }

    static List<Product> createTable(int size) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            products.add(new Product());
        }
        return products;
    }

    static CellData product2Cell(Product product, int col) {
        switch (col) {
            case 0:
                return CellData.text(product.name);
            case 1:
                return CellData.text(product.description);
            case 2:
                return CellData.number(product.price);
            case 3:
                return CellData.number(product.quantity);
            case 4:
                return CellData.date(product.createTime);
            default:
                return CellData.blank();
        }
    }

    static Object parseCell(CellData data) {
        Object output;
        if (data.isNumber()) {
            output = data.getNumberValue();
        } else if (data.isDate()) {
            output = data.getDateValue();
        } else {
            output = data.getTextValue();
        }
        return output;
    }
}

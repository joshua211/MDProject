package org.myorg.FlinkAggregation;

public  class MyProperties {
    private double price;

    public MyProperties(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "{" +
                "\"price\":" + price +
                '}';
    }
}

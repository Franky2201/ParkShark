package com.bordeauxbrothers.parkshark.person.domain.persondetails;

public enum Membership {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold");

    private final String label;
//    private final BigDecimal reduction;
//    private final int maxAllocationTime;

    Membership(String label){
        this.label = label;
//        this.reduction = reduction;
//        this.maxAllocationTime = maxAllocationTime;
    }

    public String getLabel() {
        return label;
    }
//    public BigDecimal getReduction() {
//        return reduction;
//    }
//    public int getMaxAllocationTime() {
//        return maxAllocationTime;
//    }
}

package com.bordeauxbrothers.parkshark.person.domain.persondetails;

public enum Membership {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold");

    private final String label;

    Membership(String label){
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

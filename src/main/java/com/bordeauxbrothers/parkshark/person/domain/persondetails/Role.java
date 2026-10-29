package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import java.util.List;

public enum Role {
    USER(List.of(Feature.CREATE_PARKING_LOT_ALLOCATION, Feature.STOP_PARKING_LOT_ALLOCATION, Feature.GET_MONTHLY_INVOICE)),
    ADMIN(List.of(Feature.CREATE_MANAGER, Feature.SEE_ALL_MANAGERS)),
    MANAGER(List.of(Feature.SEE_ALL_DIVISIONS, Feature.SEE_ALL_MEMBERS, Feature.SEE_SPECIFIC_DIVISION, Feature.SEE_SPECIFIC_MEMBER, Feature.CREATE_SUB_DIVISION, Feature.CREATE_DIVISION, Feature.SEE_ALL_PARKING_LOTS, Feature.SEE_SPECIFIC_PARKING_LOT, Feature.SEE_MEMBER_PARKINGlOT_ALLOCATIONS, Feature.SEE_ALLOCATIONS_FOR_SPECIFIC_PARKING_LOT, Feature.SEE_ALL_INVOICES, Feature.SEE_CLOSED_INVOICES));

    private List<Feature> features;

    Role(List<Feature> features) {
        this.features = features;
    }
    public boolean hasFeature(Feature feature) {
        return this.features.contains(feature);
    }
}

package com.bordeauxbrothers.parkshark.person.service.dtos;

public class MembershipDTO {
    private String membership;

    public MembershipDTO(String membership) {
        this.membership = membership;
    }

    public String getMembership() {
        return membership;
    }

    public void setMembership(String membership) {
        this.membership = membership;
    }
}

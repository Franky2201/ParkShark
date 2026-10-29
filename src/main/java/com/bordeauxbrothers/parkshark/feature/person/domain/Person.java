package com.bordeauxbrothers.parkshark.feature.person.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public abstract class Person {
    @Id
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}

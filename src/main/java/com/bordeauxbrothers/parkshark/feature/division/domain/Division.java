package com.bordeauxbrothers.parkshark.feature.division.domain;

import com.bordeauxbrothers.parkshark.feature.person.domain.Person;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Division {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "division name should not be blank")
    private String name;

    @NotBlank(message = "division original name should not be blank")
    private String originalName;

    @NotNull(message = "division should have a director")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "director_id", nullable = false)
    private Person director;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_division_id")
    private Division parentDivision;

    @OneToMany(mappedBy = "parentDivision")
    private List<Division> subdivisions = new ArrayList<>();

    public Division(String name, String originalName, Person director) {
        this.name = name;
        this.originalName = originalName;
        this.director = director;
    }

    public Division() {

    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Person getDirector() {
        return director;
    }

    public List<Division> getSubdivisions() {
        return subdivisions;
    }

    public void addSubdivision(Division division) {
        subdivisions.add(division);
    }
}

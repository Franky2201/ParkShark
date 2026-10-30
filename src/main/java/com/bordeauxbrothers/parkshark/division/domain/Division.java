package com.bordeauxbrothers.parkshark.division.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

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

    @Column(name = "director_id", nullable = false)
    private Integer directorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_division_id")
    private Division parentDivision;

    @OneToMany(mappedBy = "parentDivision")
    private List<Division> subdivisions = new ArrayList<>();

    public Division(String name, String originalName, Integer director) {
        this.name = name;
        this.originalName = originalName;
        this.directorId = director;
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

    public Integer getDirectorId() {
        return directorId;
    }

    public List<Division> getSubdivisions() {
        return subdivisions;
    }

    /**
     * @return the id of the parent division, or {@code null} for a top-level division.
     */
    public Integer getParentDivisionId() {
        return parentDivision == null ? null : parentDivision.getId();
    }

    public boolean isSubdivision() {
        return parentDivision != null;
    }

    /**
     * Makes {@code division} a subdivision of this division.
     * Keeps both sides of the bidirectional relation in sync.
     */
    public void addSubdivision(Division division) {
        if (division == null) {
            throw new IllegalArgumentException("subdivision should not be null");
        }
        if (division == this) {
            throw new IllegalArgumentException("a division cannot be its own subdivision");
        }
        subdivisions.add(division);
        division.parentDivision = this;
    }
}

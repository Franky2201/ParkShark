package com.bordeauxbrothers.parkshark.division.repository;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DivisionRepository extends JpaRepository<Division, Integer> {
    @EntityGraph(attributePaths = "parentDivision")
    List<Division> findAllBy();
}

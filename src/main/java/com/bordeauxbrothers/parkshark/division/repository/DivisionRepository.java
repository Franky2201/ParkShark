package com.bordeauxbrothers.parkshark.division.repository;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisionRepository extends JpaRepository<Division, Integer> {
}

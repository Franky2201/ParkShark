package com.bordeauxbrothers.parkshark.person.repositories;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicensePlateRepository extends JpaRepository<LicensePlate, Integer> {
}

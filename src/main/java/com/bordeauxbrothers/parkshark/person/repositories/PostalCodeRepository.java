package com.bordeauxbrothers.parkshark.person.repositories;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostalCodeRepository extends JpaRepository<PostalCode, Integer> {
    Optional<PostalCode> findByZipCodeAndLabel(String zipCode, String label);
}

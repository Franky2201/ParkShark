package com.bordeauxbrothers.parkshark.person.repository;

import com.bordeauxbrothers.parkshark.person.domain.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long> {
    Optional<Person> findByEmail(String email);
}

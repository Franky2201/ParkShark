package com.bordeauxbrothers.parkshark.person.repositories;

import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {
    Optional<Person> findByEmail(String email);

    @Query("SELECT m FROM Member m")
    List<Member> getAllMembers();
}

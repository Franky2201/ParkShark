package com.bordeauxbrothers.parkshark.person.repositories;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Integer> {
}

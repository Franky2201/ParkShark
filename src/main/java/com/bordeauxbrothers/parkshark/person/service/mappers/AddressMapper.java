package com.bordeauxbrothers.parkshark.person.service.mappers;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddressDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.PostalCodeDTO;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    /**
     * The postal code is passed in already resolved (found or persisted),
     * because (zip code, label) is unique in the database.
     */
    public Address toEntity(AddressDTO addressDTO, PostalCode postalCode) {
        return new Address(addressDTO.street(), addressDTO.number(), postalCode);
    }

    public AddressDTO toDto(Address address) {
        return new AddressDTO(address.getStreet(), address.getNumber(), toDto(address.getPostalCode()));
    }

    public PostalCodeDTO toDto(PostalCode postalCode) {
        return new PostalCodeDTO(postalCode.getZipCode(), postalCode.getLabel());
    }
}

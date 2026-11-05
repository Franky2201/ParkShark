package com.bordeauxbrothers.parkshark.person.service.mappers;

import com.bordeauxbrothers.parkshark.person.api.dtos.AddressDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.PostalCodeDTO;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {
    Logger logger = LoggerFactory.getLogger(AddressMapper.class);

    public Address toEntity(AddressDTO addressDTO, PostalCode postalCode) {
        logger.debug("Mapping AddressDTO to Address");
        return new Address(addressDTO.street(), addressDTO.number(), postalCode);
    }

    public AddressDTO toDto(Address address) {
        logger.debug("Mapping Address to AddressDTO");
        return new AddressDTO(address.getStreet(), address.getNumber(), toDto(address.getPostalCode()));
    }

    public PostalCodeDTO toDto(PostalCode postalCode) {
        logger.debug("Mapping PostalCode to PostalCodeDTO");
        return new PostalCodeDTO(postalCode.getZipCode(), postalCode.getLabel());
    }
}

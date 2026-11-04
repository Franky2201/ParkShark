package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.service.dtos.PostalCodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostalCodeService {

    private final PostalCodeRepository postalCodeRepository;

    public PostalCodeService(PostalCodeRepository postalCodeRepository) {
        this.postalCodeRepository = postalCodeRepository;
    }

    /**
     * Postal codes are shared: (zip code, label) is unique in the database,
     * so an existing one is reused instead of inserting a duplicate.
     */
    public PostalCode findOrCreate(PostalCodeDTO postalCodeDTO) {
        return postalCodeRepository.findByZipCodeAndLabel(postalCodeDTO.getZipCode(), postalCodeDTO.getLabel())
                .orElseGet(() -> postalCodeRepository.save(
                        new PostalCode(postalCodeDTO.getZipCode(), postalCodeDTO.getLabel())));
    }
}

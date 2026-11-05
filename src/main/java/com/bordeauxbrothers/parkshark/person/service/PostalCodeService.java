package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.api.dtos.PostalCodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostalCodeService {

    private final PostalCodeRepository postalCodeRepository;

    public PostalCodeService(PostalCodeRepository postalCodeRepository) {
        this.postalCodeRepository = postalCodeRepository;
    }

    public PostalCode findOrCreate(PostalCodeDTO postalCodeDTO) {
        return postalCodeRepository.findByZipCodeAndLabel(postalCodeDTO.zipCode(), postalCodeDTO.label())
                .orElseGet(() -> postalCodeRepository.save(
                        new PostalCode(postalCodeDTO.zipCode(), postalCodeDTO.label())));
    }
}

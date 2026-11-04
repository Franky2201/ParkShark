package com.bordeauxbrothers.parkshark.parkingLot.service;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.service.DivisionMapper;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotOverviewDto;
import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.service.mappers.AddressMapper;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotMapper {

    private final AddressMapper addressMapper;
    private final DivisionMapper divisionMapper;

    public ParkingLotMapper(AddressMapper addressMapper, DivisionMapper divisionMapper) {
        this.addressMapper = addressMapper;
        this.divisionMapper = divisionMapper;
    }

    public ParkingLot toEntity(CreateParkingLotDto dto, PostalCode postalCode, Member contactPerson, Division division) {
        return new ParkingLot(dto.name(), dto.category(), dto.maxCapacity(), dto.pricePerHour(),
                contactPerson, addressMapper.toEntity(dto.address(), postalCode), division);
    }

    public ParkingLotDto toDto(ParkingLot parkingLot) {
        return new ParkingLotDto(
                parkingLot.getId(),
                parkingLot.getName(),
                parkingLot.getCategory(),
                parkingLot.getMaxCapacity(),
                parkingLot.getPricePerHour(),
                parkingLot.getContactPerson().getId(),
                divisionMapper.toDto(parkingLot.getDivision()),
                addressMapper.toDto(parkingLot.getAddress()));
    }

    public ParkingLotOverviewDto toOverviewDto(ParkingLot parkingLot) {
        return new ParkingLotOverviewDto(
                parkingLot.getId(),
                parkingLot.getName(),
                parkingLot.getMaxCapacity(),
                parkingLot.getContactPerson().getEmail(),
                parkingLot.getContactPerson().getOnePhoneNumber());
    }
}

package com.bordeauxbrothers.parkshark.parkingLot.service;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("ParkingLotController - Integration Tests")
class ParkingLotServiceTest {

    @LocalServerPort
    private int port;


}
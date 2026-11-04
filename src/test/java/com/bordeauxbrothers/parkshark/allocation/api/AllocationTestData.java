package com.bordeauxbrothers.parkshark.allocation.api;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** Test-only SQL fixtures. Not a Spring component and not used by production code. */
final class AllocationTestData {
    static final int CREATED = 201;
    static final int OTHER_MEMBER = 930003;
    static final int SECOND_MEMBER = 950001;
    // Deliberately different IDs: using memberId to look up a lot must fail.
    static final int MEMBER = 930001;
    static final int MANAGER = 930002;
    static final int LOT = 930101;
    static final int OTHER_LOT = 930102;
    static final int DIVISION = 930201;
    static final int POSTAL = 930301;
    static final String EMAIL = "member@psa1-test.example";
    static final String PASSWORD = "TestPassword123!";
    static final String PLATE = "PSA1-001";
    static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    static final String MANAGER_EMAIL = "manager@psa1-test.example";
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    AllocationTestData(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    void reset() {
        cleanUp();
        jdbc.update("INSERT INTO parkshark.postal_code (id, zip_code, label) VALUES (?, '9000', 'PSA1 City')", POSTAL);
        for (int id : new int[]{MEMBER, LOT, OTHER_LOT}) {
            jdbc.update("INSERT INTO parkshark.address (id, street, number, postal_code_id) VALUES (?, 'Test Street', ?, ?)",
                    id, String.valueOf(id), POSTAL);
        }
        member(MEMBER, EMAIL, PLATE);
        jdbc.update("""
                INSERT INTO parkshark.person
                (id, first_name, last_name, email, password_hash, registration_date, role)
                VALUES (?, 'Test', 'Manager', ?, ?, CURRENT_DATE, 'MANAGER')
                """, MANAGER, MANAGER_EMAIL, passwordEncoder.encode(PASSWORD));
        jdbc.update("""
                INSERT INTO parkshark.division (id, name, original_name, director_id)
                VALUES (?, 'PSA1 Division', 'PSA1 Original', ?)
                """, DIVISION, MANAGER);
        for (int id : new int[]{LOT, OTHER_LOT}) {
            jdbc.update("""
                    INSERT INTO parkshark.parking_lot
                    (id, name, category, max_capacity, price_per_hour_in_euro, address_id, division_id, contact_person_id)
                    VALUES (?, ?, 'UNDERGROUND', 2, 2.50, ?, ?, ?)
                    """, id, "PSA1 Lot " + id, id, DIVISION, MANAGER);
        }
    }

    void cleanUp() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:psa1_e2e");
            return null;
        });
        jdbc.update("DELETE FROM parkshark.parking_spot_allocation WHERE parking_lot_id IN (?, ?)", LOT, OTHER_LOT);
        jdbc.update("DELETE FROM parkshark.parking_lot WHERE id IN (?, ?)", LOT, OTHER_LOT);
        jdbc.update("DELETE FROM parkshark.division WHERE id = ?", DIVISION);
        jdbc.update("DELETE FROM parkshark.person WHERE id IN (?, ?, ?, ?)", MEMBER, MANAGER, OTHER_MEMBER, SECOND_MEMBER);
        jdbc.update("DELETE FROM parkshark.license_plate WHERE id IN (?, ?, ?)", MEMBER, OTHER_MEMBER, SECOND_MEMBER);
        jdbc.update("DELETE FROM parkshark.address WHERE id IN (?, ?, ?)", MEMBER, LOT, OTHER_LOT);
        jdbc.update("DELETE FROM parkshark.postal_code WHERE id = ?", POSTAL);

    }

    void member(int id, String email, String plate) {
        jdbc.update("INSERT INTO parkshark.license_plate (id, number, country) VALUES (?, ?, 'BE')", id, plate);
        jdbc.update("""
                INSERT INTO parkshark.person
                (id, first_name, last_name, email, password_hash, registration_date, role,
                 address_id, phone_number, license_plate_id, membership)
                VALUES (?, 'Test', 'Member', ?, ?, CURRENT_DATE, 'USER', ?, '+32470000111', ?, 'BRONZE')
                """, id, email, passwordEncoder.encode(PASSWORD), MEMBER, id);
    }

    void membership(String value) {
        jdbc.update("UPDATE parkshark.person SET membership = ? WHERE id = ?", value, MEMBER);
    }

    int allocation(String status) {
        return allocation(LOT, status);
    }

    int allocation(int lot, String status) {
        OffsetDateTime start = hoursAgo(2);
        return insert(null, MEMBER, lot, PLATE, start,
                status.equals("ACTIVE") ? null : start.plusHours(1), status);
    }

    int allocation(int id, int lot, String status, int hoursAgo) {
        return allocation(id, MEMBER, lot, status, PLATE, hoursAgo);
    }

    int allocation(int id, int member, int lot, String status, String plate, int hoursAgo) {
        OffsetDateTime start = hoursAgo(hoursAgo);
        return insert(id, member, lot, plate, start,
                status.equals("ACTIVE") ? null : start.plusMinutes(30), status);
    }

    private int insert(Integer id, int member, int lot, String plate,
                       OffsetDateTime start, OffsetDateTime end, String status) {
        String columns = "member_id, parking_lot_id, license_plate_number, start_time, end_time, status";
        String placeholders = "?, ?, ?, ?, ?, ?";
        Object[] values = {member, lot, plate, start, end, status};
        if (id != null) {
            columns = "id, " + columns;
            placeholders = "?, " + placeholders;
            values = new Object[]{id, member, lot, plate, start, end, status};
        }
        String sql = "INSERT INTO parkshark.parking_spot_allocation (" + columns
                + ") VALUES (" + placeholders + ")";
        Object[] parameters = values;
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            return statement;
        }, keys);
        return id != null ? id : Objects.requireNonNull(keys.getKey()).intValue();
    }

    private OffsetDateTime hoursAgo(int hours) {
        return OffsetDateTime.ofInstant(NOW.minusSeconds(hours * 3600L), ZoneOffset.UTC);
    }

    long count() {
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT COUNT(*) FROM parkshark.parking_spot_allocation WHERE parking_lot_id IN (?, ?)",
                Long.class, LOT, OTHER_LOT));
    }

    Map<String, Object> row(int id) {
        return jdbc.queryForMap("SELECT * FROM parkshark.parking_spot_allocation WHERE id = ?", id);
    }

    List<Map<String, Object>> rows() {
        return jdbc.queryForList("""
                SELECT * FROM parkshark.parking_spot_allocation
                WHERE parking_lot_id IN (?, ?) ORDER BY id
                """, LOT, OTHER_LOT);
    }

    String status(int id) {
        return (String) row(id).get("status");
    }

    OffsetDateTime time(int id, String column) {
        if (!List.of("start_time", "end_time").contains(column)) {
            throw new IllegalArgumentException("Not an allocation timestamp: " + column);
        }
        return jdbc.queryForObject("SELECT " + column
                + " FROM parkshark.parking_spot_allocation WHERE id = ?", OffsetDateTime.class, id);
    }
}

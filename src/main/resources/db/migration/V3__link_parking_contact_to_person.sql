-- 1. Add a separate mobile number to person.
-- phone_number already exists.

ALTER TABLE parkshark.person
    ADD COLUMN mobile_number VARCHAR(40);


-- 2. Add the new contact-person reference.

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_person_id INTEGER;

ALTER TABLE parkshark.parking_lot
    ADD CONSTRAINT fk_parking_lot_contact_person
        FOREIGN KEY (contact_person_id)
            REFERENCES parkshark.person (id);


-- 3. Match existing contacts to persons by email.
-- Multiple matches cause an error rather than choosing arbitrarily.

UPDATE parkshark.parking_lot
SET contact_person_id = (
    SELECT p.id
    FROM parkshark.person p
    WHERE LOWER(TRIM(p.email)) = LOWER(TRIM(
            parkshark.parking_lot.contact_email
                                       ))
);


-- 4. Stop if any existing contact has no matching person.

ALTER TABLE parkshark.parking_lot
    ALTER COLUMN contact_person_id SET NOT NULL;


-- 5. Fill missing person details using the existing contact data.
-- Existing person values are preserved.
-- For shared contacts, the parking with the lowest ID is used.

UPDATE parkshark.person
SET mobile_number = COALESCE(
        NULLIF(TRIM(mobile_number), ''),
        (
            SELECT pl.contact_mobile_number
            FROM parkshark.parking_lot pl
            WHERE pl.contact_person_id = parkshark.person.id
              AND NULLIF(TRIM(pl.contact_mobile_number), '') IS NOT NULL
            ORDER BY pl.id
                FETCH FIRST 1 ROW ONLY
        )
                    ),
    phone_number = COALESCE(
            NULLIF(TRIM(phone_number), ''),
            (
                SELECT pl.contact_phone_number
                FROM parkshark.parking_lot pl
                WHERE pl.contact_person_id = parkshark.person.id
                  AND NULLIF(TRIM(pl.contact_phone_number), '') IS NOT NULL
                ORDER BY pl.id
                    FETCH FIRST 1 ROW ONLY
            )
                   ),
    address_id = COALESCE(
            address_id,
            (
                SELECT pl.contact_address_id
                FROM parkshark.parking_lot pl
                WHERE pl.contact_person_id = parkshark.person.id
                ORDER BY pl.id
                    FETCH FIRST 1 ROW ONLY
            )
                 )
WHERE EXISTS (
    SELECT 1
    FROM parkshark.parking_lot pl
    WHERE pl.contact_person_id = parkshark.person.id
);


-- 6. Index for searching parking lots by contact person.

CREATE INDEX idx_parking_lot_contact_person
    ON parkshark.parking_lot (contact_person_id);


-- 7. Remove the old embedded contact details.

ALTER TABLE parkshark.parking_lot
DROP CONSTRAINT chk_parking_lot_contact_phone;

ALTER TABLE parkshark.parking_lot
DROP CONSTRAINT fk_parking_lot_contact_address;

DROP INDEX parkshark.idx_parking_lot_contact_address;

ALTER TABLE parkshark.parking_lot
DROP COLUMN contact_first_name;

ALTER TABLE parkshark.parking_lot
DROP COLUMN contact_last_name;

ALTER TABLE parkshark.parking_lot
DROP COLUMN contact_email;

ALTER TABLE parkshark.parking_lot
DROP COLUMN contact_mobile_number;

ALTER TABLE parkshark.parking_lot
DROP COLUMN contact_phone_number;

ALTER TABLE parkshark.parking_lot
DROP COLUMN contact_address_id;
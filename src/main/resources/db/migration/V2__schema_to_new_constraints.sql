-- =========================================================
-- 1. Add member-specific fields to the shared person table.
-- These fields may remain NULL for admins and managers.
-- =========================================================

ALTER TABLE parkshark.person
    ADD COLUMN address_id INTEGER;

ALTER TABLE parkshark.person
    ADD COLUMN phone_number VARCHAR(40);

ALTER TABLE parkshark.person
    ADD COLUMN license_plate_id INTEGER;

ALTER TABLE parkshark.person
    ADD COLUMN membership VARCHAR(20);


-- =========================================================
-- 2. Copy existing member data.
-- In V1, member.id is also the corresponding person.id.
-- =========================================================

UPDATE parkshark.person
SET address_id = (
        SELECT m.address_id
        FROM parkshark.member m
        WHERE m.id = parkshark.person.id
    ),
    phone_number = (
        SELECT m.phone_number
        FROM parkshark.member m
        WHERE m.id = parkshark.person.id
    ),
    license_plate_id = (
        SELECT m.license_plate_id
        FROM parkshark.member m
        WHERE m.id = parkshark.person.id
    ),
    membership = (
        SELECT m.membership
        FROM parkshark.member m
        WHERE m.id = parkshark.person.id
    )
WHERE EXISTS (
    SELECT 1
    FROM parkshark.member m
    WHERE m.id = parkshark.person.id
);


-- =========================================================
-- 3. Add relationships and validation to person.
-- USER means a member in your current role model.
-- =========================================================

ALTER TABLE parkshark.person
    ADD CONSTRAINT fk_person_address
        FOREIGN KEY (address_id)
        REFERENCES parkshark.address (id);

ALTER TABLE parkshark.person
    ADD CONSTRAINT fk_person_license_plate
        FOREIGN KEY (license_plate_id)
        REFERENCES parkshark.license_plate (id);

ALTER TABLE parkshark.person
    ADD CONSTRAINT uq_person_license_plate
        UNIQUE (license_plate_id);

ALTER TABLE parkshark.person
    ADD CONSTRAINT chk_person_membership
        CHECK (
            membership IS NULL
            OR membership IN ('BRONZE', 'SILVER', 'GOLD')
        );

ALTER TABLE parkshark.person
    ADD CONSTRAINT chk_person_member_details
        CHECK (
            role <> 'USER'
            OR (
                address_id IS NOT NULL
                AND NULLIF(TRIM(phone_number), '') IS NOT NULL
                AND license_plate_id IS NOT NULL
                AND membership IS NOT NULL
            )
        );

CREATE INDEX idx_person_address
    ON parkshark.person (address_id);


-- =========================================================
-- 4. Point existing allocations and invoices to person.
-- Existing member IDs remain unchanged.
-- =========================================================

ALTER TABLE parkshark.parking_spot_allocation
    DROP CONSTRAINT fk_allocation_member;

ALTER TABLE parkshark.parking_spot_allocation
    ADD CONSTRAINT fk_allocation_member
        FOREIGN KEY (member_id)
        REFERENCES parkshark.person (id);

ALTER TABLE parkshark.invoice
    DROP CONSTRAINT fk_invoice_member;

ALTER TABLE parkshark.invoice
    ADD CONSTRAINT fk_invoice_member
        FOREIGN KEY (member_id)
        REFERENCES parkshark.person (id);


-- =========================================================
-- 5. Remove the old member table.
-- Its data has been transferred to person.
-- =========================================================

DROP TABLE parkshark.member;


-- =========================================================
-- 6. Add embedded contact-person fields to parking_lot.
-- contact_address_id is separate from the parking's address.
-- =========================================================

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_first_name VARCHAR(100);

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_last_name VARCHAR(100);

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_email VARCHAR(254);

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_mobile_number VARCHAR(40);

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_phone_number VARCHAR(40);

ALTER TABLE parkshark.parking_lot
    ADD COLUMN contact_address_id INTEGER;


-- =========================================================
-- 7. Copy each parking's existing contact details.
-- =========================================================

UPDATE parkshark.parking_lot
SET contact_first_name = (
        SELECT c.first_name
        FROM parkshark.contact_person c
        WHERE c.id = parkshark.parking_lot.contact_person_id
    ),
    contact_last_name = (
        SELECT c.last_name
        FROM parkshark.contact_person c
        WHERE c.id = parkshark.parking_lot.contact_person_id
    ),
    contact_email = (
        SELECT c.email
        FROM parkshark.contact_person c
        WHERE c.id = parkshark.parking_lot.contact_person_id
    ),
    contact_mobile_number = (
        SELECT c.mobile_number
        FROM parkshark.contact_person c
        WHERE c.id = parkshark.parking_lot.contact_person_id
    ),
    contact_phone_number = (
        SELECT c.phone_number
        FROM parkshark.contact_person c
        WHERE c.id = parkshark.parking_lot.contact_person_id
    ),
    contact_address_id = (
        SELECT c.address_id
        FROM parkshark.contact_person c
        WHERE c.id = parkshark.parking_lot.contact_person_id
    );


-- =========================================================
-- 8. Make required contact fields mandatory after copying.
-- =========================================================

ALTER TABLE parkshark.parking_lot
    ALTER COLUMN contact_first_name SET NOT NULL;

ALTER TABLE parkshark.parking_lot
    ALTER COLUMN contact_last_name SET NOT NULL;

ALTER TABLE parkshark.parking_lot
    ALTER COLUMN contact_email SET NOT NULL;

ALTER TABLE parkshark.parking_lot
    ALTER COLUMN contact_address_id SET NOT NULL;

ALTER TABLE parkshark.parking_lot
    ADD CONSTRAINT fk_parking_lot_contact_address
        FOREIGN KEY (contact_address_id)
        REFERENCES parkshark.address (id);

ALTER TABLE parkshark.parking_lot
    ADD CONSTRAINT chk_parking_lot_contact_phone
        CHECK (
            NULLIF(TRIM(contact_mobile_number), '') IS NOT NULL
            OR
            NULLIF(TRIM(contact_phone_number), '') IS NOT NULL
        );

CREATE INDEX idx_parking_lot_contact_address
    ON parkshark.parking_lot (contact_address_id);


-- =========================================================
-- 9. Remove the old contact relationship and table.
-- =========================================================

ALTER TABLE parkshark.parking_lot
    DROP CONSTRAINT fk_parking_lot_contact_person;

DROP INDEX parkshark.idx_parking_lot_contact;

ALTER TABLE parkshark.parking_lot
    DROP COLUMN contact_person_id;

DROP TABLE parkshark.contact_person;

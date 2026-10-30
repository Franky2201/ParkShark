-- 1. Remove the old constraint
ALTER TABLE parkshark.person
DROP CONSTRAINT chk_person_member_details;

-- 2. Add the updated constraint without the phone number check
ALTER TABLE parkshark.person ADD CONSTRAINT chk_person_member_details CHECK (
    role <> 'USER' OR (
        address_id IS NOT NULL
            AND license_plate_id IS NOT NULL
            AND membership IS NOT NULL
        )
    );
-- =========================================================
-- 1. Add the parent reference.
-- NULL means this division has no parent.
-- =========================================================

ALTER TABLE parkshark.division
    ADD COLUMN parent_division_id INTEGER;


-- =========================================================
-- 2. Reverse existing parent -> child references.
--
-- Before: division 1 has subdivision_id = 2.
-- After:  division 2 has parent_division_id = 1.
--
-- If several divisions reference the same child, this scalar
-- subquery fails rather than choosing a parent arbitrarily.
-- =========================================================

UPDATE parkshark.division
SET parent_division_id = (
    SELECT parent.id
    FROM parkshark.division parent
    WHERE parent.subdivision_id = parkshark.division.id
);


-- =========================================================
-- 3. Add the new foreign key and self-reference check.
-- =========================================================

ALTER TABLE parkshark.division
    ADD CONSTRAINT fk_division_parent
        FOREIGN KEY (parent_division_id)
            REFERENCES parkshark.division (id);

ALTER TABLE parkshark.division
    ADD CONSTRAINT chk_division_not_own_parent
        CHECK (parent_division_id <> id);


-- =========================================================
-- 4. Index for finding all children of a division.
-- Deliberately NOT UNIQUE: several children may share a parent.
-- =========================================================

CREATE INDEX idx_division_parent
    ON parkshark.division (parent_division_id);


-- =========================================================
-- 5. Remove the old child reference.
-- =========================================================

ALTER TABLE parkshark.division
DROP CONSTRAINT fk_subdivision;

ALTER TABLE parkshark.division
DROP CONSTRAINT chk_division_not_own_child;

DROP INDEX parkshark.idx_subdivision;

ALTER TABLE parkshark.division
DROP COLUMN subdivision_id;
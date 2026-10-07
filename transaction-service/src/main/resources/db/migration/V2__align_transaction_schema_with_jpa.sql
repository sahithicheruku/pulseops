ALTER TABLE transaction
    ALTER COLUMN amount TYPE NUMERIC(38, 2),
    ALTER COLUMN status TYPE SMALLINT
        USING CASE status
            WHEN 'RECEIVED' THEN 0
            WHEN 'VALIDATING' THEN 1
            WHEN 'PROCESSING' THEN 2
            WHEN 'COMPLETED' THEN 3
            WHEN 'FAILED' THEN 4
            ELSE status::SMALLINT
        END;

ALTER TABLE transaction
    ALTER COLUMN status SET NOT NULL;

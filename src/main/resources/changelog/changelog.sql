CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS product_changelog (
    id uuid not null
        constraint product_changelog_pk primary key,
    version integer not null,
    prev_changelog_id uuid,
    changed_by text not null,
    ts timestamp default now() not null,
    product_id bigint NOT NULL,
    name text NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS product_changelog_product_id
    on product_changelog(product_id, version);

/**
  Function to return the current user.
 */
DROP FUNCTION IF EXISTS  sample_get_user();
CREATE OR REPLACE FUNCTION sample_get_user()
    RETURNS text AS $$

DECLARE
    user_id text := current_setting('sample.user_id', true);
BEGIN
    IF user_id IS NOT NULL THEN
        return user_id;
    ELSE
        return 'system';
    END IF;
END
$$ LANGUAGE 'plpgsql';

/**
  Function to log a change after insert or update on product
 */
CREATE OR REPLACE FUNCTION product_log_change() RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    change_log   uuid := uuid_generate_v4();
    next_version int  := NULL;
    is_exists    bool := FALSE;
BEGIN
    NEW.changelog_id = change_log;
--     Check whether this is an UPSERT CALL (insert on conflict)
    BEGIN
        SELECT true
        FROM product
        WHERE id = NEW.id
        INTO is_exists;
    END;
    IF is_exists = TRUE AND TG_OP = 'INSERT'
    THEN
        RETURN NEW;
    END IF;
    BEGIN
        SELECT product_changelog.version + 1
        FROM product_changelog
        WHERE product_id = NEW.id
        ORDER BY version desc
        LIMIT 1
        INTO STRICT next_version;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            next_version = 1;
    END;
    NEW.version = next_version;
    INSERT INTO product_changelog
        (id, version, prev_changelog_id, changed_by, ts, product_id, name)
    VALUES (change_log, NEW.version, OLD.changelog_id, sample_get_user(), now(),
            NEW.id, NEW.name);
    RETURN NEW;
END;
$$;


/**
  Function to log a change after a delete on product
 */
CREATE OR REPLACE FUNCTION product_log_delete()
    RETURNS TRIGGER AS
$$
DECLARE
    change_log uuid := uuid_generate_v4();
BEGIN
    INSERT INTO product_changelog
        (id, version, prev_changelog_id, changed_by, ts, product_id, name)
    VALUES (change_log, 0 - OLD.version, OLD.changelog_id, sample_get_user(), now(),
            OLD.id, OLD.name);
    RETURN OLD;
END;
$$ LANGUAGE 'plpgsql';

-- Create Triggers for product insert/update/deletes
drop trigger if exists product_changed ON product;
create trigger product_changed
    before insert OR update
    on product
    for each row
execute function product_log_change();

drop trigger if exists product_deleted ON product;
create trigger product_deleted
    before delete
    on product
    for each row
execute function product_log_delete();

CREATE TABLE IF NOT EXISTS product (
    id bigserial not null unique,
    name text not null,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    version integer default 0 not null,
    changelog_id uuid,
    primary key (id)
);
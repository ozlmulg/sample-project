## Sample Project

### Build and Run

This will clean and force update dependencies

    mvn clean install -U

to build: in the project root, run `make mvn-build`

to run: in the project root, run `make mvn-run`

### Local DB Setup

Sample App uses Postgres for its data storage. Docker is the only dependency to run Postgres locally, below
related `make` commands to get started.

```bash
# Start Postgres Service locally with docker
make postgresql-start

# Stop Postgres Service locally with docker
make postgresql-stop

# Generate entities from sql scripts by using jooq codegen under resources/db/sql
make jooq-codegen

# Drop and recreate local database on docker
# Import table schema to local database under resources/db/sql
# Import test data to local database under resources/data
# Import changelog table and triggers to local database under resources/changelog
make create-database

```

Sample db credentials:

```properties
username=postgres
password=admin
host=localhost
port=5435
```

Steps to create local database:

- Run `make create-database` command before running service app to create your database on docker.
- If the postgresql could not be started yet, please run `make create-database` command again. It can take some time to
  start postgresql by `make postgresql-start` command. You can use `make postgresql-stop` make command if you see any
  problem about database.
- Run `make jooq-codegen` command if you have any sql file changes on modules under db/sql. Jooq will regenerate
  entities from sql scripts. You should not change entities manually.
- Start service app by running Application.java.

### Tables

- `product`:

Keeps product data in this table by their id and name.

```postgresql
CREATE TABLE IF NOT EXISTS product (
    id bigserial not null unique,
    name text not null,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    version integer default 0 not null,
    changelog_id uuid,
    primary key (id)
);
```

- `product_changelog`:

The postgresql triggers (product_changed and product_deleted) listen product table upserts and delete operations.
And insert a changelog history record to product_changelog table.
We also set the changed_by column in this table in the trigger function by selecting `sample.user_id` from postgresql
current_settings that we will set this variable by using setLocal in a transaction.

```postgresql
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
```

### API Specification

http://localhost:8080

### Reproduce Jooq Transaction and Set Local Problem

Steps:

- Call the below API endpoints to test you want to debug.
- Check product table whether it is updated or not.
- Check changed_by column on product_changelog table whether it is set or not.

#### getByIdWithJdbc

Endpoint: http://localhost:8080/products/jdbc/1
Description: Call this API to test dslContext.select works with jdbc

#### getByIdWithR2dbc

Endpoint: http://localhost:8080/products/r2dbc/1
Description: Call this API to test dslContext.select works with r2dbc

#### upsertWithJdbc

Endpoint: http://localhost:8080/products/jdbc/upsert/1?name=upsertWithJdbc&userId=123
Description:

- dslContext.setLocal does not work with jdbc.
- dslContext.insert works successfully.
- We can see updated product on product table.
- But we don't see changed_by column on product_changelog table is empty even though we set it in
  dslContext.transactionResult

#### upsertWithR2dbc

Endpoint: http://localhost:8080/products/r2dbc/upsert/1?name=upsertWithR2dbc&userId=123
Description:

- dslContext.setLocal does not work with r2dbc.
- dslContext.insert does not work with r2dbc.
- It does not commit the scripts to database.
- We can not see any updated product on product table or changelog on product_changelog table.

#### upsertWithJdbcBegin

Endpoint: http://localhost:8080/products/jdbc/begin/upsert/1?name=upsertWithJdbcBegin&userId=123
Description:

- dslContext.begin works with jdbc for both of setLocal and insert statements.
- We can see updated product on product table.
- We can see changed_by column as userId parameter on product_changelog table.
- But we will always need to fetch the product from table because dslContext.begin don't return updated record.

#### upsertWithR2dbcBegin

Endpoint: http://localhost:8080/products/r2dbc/begin/upsert/1?name=upsertWithR2dbcBegin&userId=123
Description:

- dslContext.begin works with r2dbc for both of setLocal and insert statements.
- We can see updated product on product table.
- We can see changed_by column as userId parameter on product_changelog table.
- But we will always need to fetch the product from table because dslContext.begin don't return updated record.

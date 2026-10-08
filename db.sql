create table if not exists product_catalog
(
    id         int generated always as identity primary key,
    barcode    varchar(32)  not null unique,
    name       varchar(255) not null,
    source     varchar(32)  not null check (source in ('OPENFOODFACTS', 'USER_CONFIRMED', 'MANUAL')),
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create table if not exists app_user
(
    id            int generated always as identity primary key,
    email         varchar(255) not null unique,
    password_hash text         not null,
    created_at    timestamptz  not null default now()
);

create table if not exists user_inventory
(
    id              uuid primary key     default gen_random_uuid(),
    user_id         int         not null,
    product_id      int         null,
    custom_name     varchar     null,
    expiration_date date        not null,
    consumed_at     timestamptz null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    deleted_at      timestamptz null,

    constraint fk_users_inventories
        foreign key (user_id)
            references app_user (id)
            on delete cascade,

    constraint fk_products_inventories
        foreign key (product_id)
            references product_catalog (id)
            on delete restrict,

    check ((product_id is not null and custom_name is null)
        or (product_id is null and custom_name is not null))
);

create table if not exists product_candidate
(
    id         int generated always as identity primary key,
    barcode    varchar(32)  not null,
    name       varchar(255) not null,
    status     varchar(32)  not null default 'PENDING' check (status in ('PENDING', 'APPROVED', 'REJECTED', 'MERGED')),
    user_id    int          null,
    created_at timestamptz  not null default now(),

    constraint fk_users_product_candidates
        foreign key (user_id)
            references app_user (id)
            on delete set null

);

create index if not exists product_candidate_barcode_idx on product_candidate (barcode);
create index if not exists product_candidate_user_id_idx on product_candidate (user_id);
create index if not exists user_inventory_user_id_idx on user_inventory (user_id);
create index if not exists user_inventory_product_id_idx on user_inventory (product_id);
create index if not exists user_inventory_active_idx on user_inventory (user_id) where consumed_at is null and deleted_at is null;

create table follower_subscription(
    id bigserial primary key,
    origin bytea not null,
    expires_at bigint not null,
    environment_id bigint not null,
    callbacks int
);

create index ix_follower_subscription_origin on follower_subscription(origin);
create index ix_follower_subscription_expires_at on follower_subscription(expires_at);
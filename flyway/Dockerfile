FROM flyway/flyway:11

COPY --chown=10001:10001 migration /flyway/sql

ENV FLYWAY_LOCATIONS=filesystem:/flyway/sql \
    FLYWAY_CONNECT_RETRIES=10

USER 10001:10001

CMD ["migrate"]
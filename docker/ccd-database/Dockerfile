FROM postgres:17.6

# Change to non-root privilege
USER postgres

COPY init-db.sh /docker-entrypoint-initdb.d

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d ccd_data -U ${DB_USERNAME}

EXPOSE 5432

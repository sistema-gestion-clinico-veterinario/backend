# Database migration policy

Hibernate validates the schema and must not modify production tables. Database
changes are applied explicitly before an incompatible application version is
started.

## Current limitation

The repository contains incremental scripts, but it does not contain a complete
baseline of the schema that existed before `V35`. Enabling Flyway against an
empty database or baselining production without comparing its real schema would
therefore be unsafe.

## Safe adoption procedure

1. Create a provider backup and record the restore point.
2. Export the production schema without data (`pg_dump --schema-only`).
3. Compare that schema with the JPA entities and migrations `V35` onward.
4. Produce and review a complete baseline migration from the verified schema.
5. Restore an anonymized copy into staging.
6. Apply the baseline and every incremental migration in staging.
7. Start the application with `JPA_DDL_AUTO=validate` and run API smoke tests.
8. Only then add/enable Flyway in production and record its baseline version.

Do not set `JPA_DDL_AUTO=update` in production. A deployment must fail safely
when its expected schema is missing instead of asking Hibernate to infer a
destructive migration.

## Rules for new migrations

- Never edit a migration already applied in a shared environment.
- Use the next monotonically increasing version.
- Make retry-safe changes where PostgreSQL supports `IF EXISTS`/`IF NOT EXISTS`.
- Separate destructive changes into expand/migrate/contract deployments.
- Test both a clean installation and an upgrade from the previous release.
- Document rollback or forward-fix steps for every production migration.

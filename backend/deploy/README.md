# Running the service

Each app release also publishes the service as an image, `ghcr.io/<owner>/tayra-backend:<version>`
(`.github/workflows/release.yml`). On Fly.io it runs from that image with `fly.toml` here (its
comments give the commands); on a box of its own, as below.

## On a box of its own

`selfhost.yml` runs the service as `docker-compose.yml` does for development, with three changes:
[Caddy](https://caddyserver.com) in front with HTTPS for the service's domain (it gets and renews the
certificate itself), the real S3-compatible bucket instead of S3Mock, and the database dumped to the
bucket every night. Only Caddy's ports are open; the database and the service are reached through
it alone.

### Settings

Put every setting in one file, such as `/etc/tayra.env`, readable by root only. The service reads it
whole, so anything `backend/src/main/resources/application.yaml` takes from the environment goes there.

```sh
TAYRA_DOMAIN=api.example.com          # its DNS A/AAAA record points at the box
TAYRA_ENV_FILE=/etc/tayra.env         # this file, handed to the service as it is
DATABASE_PASSWORD=…                   # any long random text
JWT_SECRET=…                          # at least 32 bytes: openssl rand -base64 48
S3_ENDPOINT=https://…                 # R2, B2 or Hetzner Object Storage
S3_REGION=auto
S3_BUCKET=tayra-backups               # cloud backups, and the database dumps under db/
S3_ACCESS_KEY=…
S3_SECRET_KEY=…
MAIL_FROM=Tayra Languages <no-reply@example.com>
SPRING_MAIL_HOST=smtp.resend.com      # leave the mail settings out entirely to only log the codes
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=…
SPRING_MAIL_PASSWORD=…
WEB_ORIGINS=https://app.example.com   # the web app's origin, if it is served
ADMIN_TOKEN=…                         # for /actuator/metrics
# Sign-in, billing and the rest: see application.yaml.
```

### Starting and updating

From a checkout of the repository on the box:

```sh
./gradlew :backend:bootJar
docker compose -f backend/docker-compose.yml -f backend/deploy/selfhost.yml --env-file /etc/tayra.env up --build -d
```

Run the same after pulling a new version; Flyway brings the database up to it as the service starts.
`docker compose … logs -f app` shows the service's log, `… logs backup` the dumps.

### Restoring the database

The dumps are `db/tayra-<time>.dump` in the bucket, in `pg_dump`'s custom format; `KEEP_DUMPS`
(14 by default) are kept. To put one back, with the service stopped:

```sh
docker compose … stop app
docker compose … exec backup sh -c 'aws --endpoint-url "$S3_ENDPOINT" --region "$S3_REGION" s3 cp "s3://$S3_BUCKET/db/<dump>" /tmp/db.dump'
docker compose … exec backup sh -c 'dropdb tayra && createdb tayra && pg_restore --no-owner -d tayra /tmp/db.dump'
docker compose … start app
```

## Moving to or from a platform

The apps reach the service by `TAYRA_DOMAIN`, never by a host's own name, so a move is: put the
newest dump into the new database (as above, or `pg_restore` into the platform's Postgres), point the
new service at the same bucket (or copy it), and change the domain's DNS record.

# PlaneR Deployment

This document describes how PlaneR is built and deployed.
Some sections describe the current plan; others are marked as **TBD** because
the corresponding pipelines or infrastructure do not exist yet.

- [Overview](#overview)
- [Environments](#environments)
- [Continuous Integration](#continuous-integration)
- [Releases](#releases)
- [Server Deployment](#server-deployment)
- [Desktop Deployment](#desktop-deployment)
- [Android Deployment](#android-deployment)
- [TLS and Reverse Proxy](#tls-and-reverse-proxy)
- [Backups](#backups)
- [Secrets](#secrets)

## Overview

PlaneR has three deployable artifacts:

| Artifact | Where it runs             | Distribution                           |
|----------|---------------------------|----------------------------------------|
| Server   | Cloud VM / container host | Docker Hub image                       |
| Desktop  | User's machine            | GitHub Releases (MSI, DMG, DEB)        |
| Android  | User's phone              | Google Play Console (internal testing) |

Each is built and published by its own GitHub Actions workflow, and **all three are triggered by the same thing: a
version tag** (see [Releases](#releases)).

## Environments

| Environment     | Purpose                            | Database                                                                                                    |
|-----------------|------------------------------------|-------------------------------------------------------------------------------------------------------------|
| **Development** | Local development                  | Docker, or local Postgres                                                                                   |
| **Testing**     | CI runs unit and integration tests | Testcontainers (ephemeral)                                                                                  |
| **Production**  | Real users                         | A Postgres container on the cloud VM, with its data on a persistent volume and backed up as described below |

There is no staging environment. For a personal project, this is acceptable —
the tests provide enough coverage that a broken deploy is caught before it ships.

## Continuous Integration

A GitHub Actions workflow runs on **every pull request** targeting `develop` or `master`, and on every push to those
branches:

1. Check out the repository and set up JDK 21 and Gradle (with caching).
2. Build every module: `./gradlew build -x test`.
3. Run unit tests: `./gradlew test`.
4. Run the server integration tests: `./gradlew :planer-server:integrationTest` (Testcontainers starts Postgres on the
   runner).
5. A pull request cannot be merged until the workflow is green.

The end-to-end UI tests for the clients are **not** part of this workflow: they need a running server and, for Android,
an emulator. They are run manually before a release (see [Releases](#releases)).

CI uses throwaway secrets generated for the run (for example a random `JWT_SECRET_KEY`); it never has access to
production secrets.

## Releases

A release is made from `master` and tagged with a semantic version (see [Git → Releases](../dev/Git.md#releases)).
Pushing the tag `v<major>.<minor>.<patch>` starts three workflows in parallel:

| Workflow | Produces                                                                                           |
|----------|----------------------------------------------------------------------------------------------------|
| Server   | A Docker image tagged with the version and `latest`, pushed to Docker Hub, then deployed to the VM |
| Desktop  | MSI, DMG, and DEB installers attached to the GitHub Release                                        |
| Android  | A signed AAB uploaded to the Play Console                                                          |

**Order matters when the contract changes.** Because the server can refuse old clients
(see [Api → Versioning and Forced Updates](../api/Api.md#versioning-and-forced-updates)), a release that breaks
compatibility is rolled out in this order:

1. Release the version. The server deploys, and must still accept the **previous** clients — so breaking changes in the
   same release are not allowed to take effect yet.
2. Wait until the new clients are actually available to users (Play review finished, installers published).
3. Raise `MIN_CLIENT_VERSION_ANDROID` / `MIN_CLIENT_VERSION_DESKTOP` in the server's configuration on the VM and restart
   the container.

Additive changes (new endpoints, new optional fields) need no step 3.

## Server Deployment

The server is deployed as a **Docker image**, built and pushed automatically when a version tag is pushed.

**Pipeline (planned):**

1. The tag workflow builds the server JAR with `./gradlew :planer-server:buildFatJar`.
2. A multi-stage `Dockerfile` packages the JAR into an image based on `eclipse-temurin:21-jre`.
3. The image is tagged with the version and `latest`, then pushed to Docker Hub.
4. The workflow connects to the VM over **SSH** (using a deploy key stored as a GitHub Secret) and runs
   `docker compose -f docker-compose.prod.yml pull` followed by `up -d`.
5. On startup, the server applies pending Flyway migrations before it serves any request.
6. The workflow polls `GET /health` until it returns `200`, and fails the deployment if it does not within a few
   minutes.

**Rolling back.** Every image is tagged with its version, so rolling back the server is pointing
`docker-compose.prod.yml` at the previous tag. Flyway cannot undo migrations
(see [Database → Rolling Back](Database.md#rolling-back)), so a release with a destructive migration also needs a
database backup taken first.

**Manual override:**

```sh
docker compose -f docker-compose.prod.yml up -d --pull always
```

**Configuration on the VM.** The production `.env` lives on the VM, next to `docker-compose.prod.yml`, and is **not** in
the repository or in GitHub. It holds the database credentials, `JWT_SECRET_KEY`, the email-delivery credentials, and
the `MIN_CLIENT_VERSION_*` values.

## Desktop Deployment

The desktop app is built for three platforms and attached to a GitHub Release.

**Pipeline (planned):**

1. A version tag (e.g., `v0.1.0`) is pushed.
2. Three parallel jobs build the installer:
    - Windows → `packageMsi`
    - macOS → `packageDmg`
    - Linux → `packageDeb`
3. The artifacts are uploaded to a new GitHub Release.
4. Users download the installer from the Releases page.

There is no auto-update mechanism in v1. Users are expected to download new
versions manually; when the server's minimum version passes theirs, the app shows its
forced-update screen pointing at the Releases page. Auto-update can be added later via
`org.beryx.jlink` or a similar tool.

## Android Deployment

The Android app is published to the **Google Play Console** on the internal
testing track.

**Pipeline (planned):**

1. A version tag is pushed.
2. A GitHub Action builds a signed AAB with `./gradlew :planer-android:bundleRelease`.
3. The AAB is uploaded to the Play Console via the `r0adkll/upload-google-play` action.
4. Testers receive an update notification.

Signing keys are stored as GitHub Secrets and never committed. Play review can take a while after upload, which is why
the minimum client version is raised only *after* the new build is live (see [Releases](#releases)).

## TLS and Reverse Proxy

The server itself listens on plain HTTP inside the container network.
TLS is terminated by a reverse proxy running on the same host.

**Proposed setup:** **Caddy**, because it handles Let's Encrypt certificate
issuance and renewal automatically with no configuration.

```
planer.example.com {
    header Strict-Transport-Security "max-age=31536000"
    reverse_proxy localhost:8080
}
```

That is the entire Caddyfile. Caddy obtains and renews the certificate on its own, adds the
HSTS header, and sets `X-Forwarded-For` on every proxied request.

The server must be configured to trust `X-Forwarded-For` **only from the proxy** (the address Caddy connects from), so
per-IP rate limits apply to real clients and the header cannot be spoofed from outside. The Ktor port is never published
on a public interface.

If Caddy is not used, **Nginx** with **Certbot** is the standard alternative.

## Backups

**TBD.** The database will need a backup strategy before there is real user data.
The likely approach is a nightly `pg_dump` piped to **restic** and uploaded to
S3-compatible object storage. Retention: 7 daily, 4 weekly, 12 monthly.

Whatever is chosen must also cover "a backup immediately before a release that contains a migration", and a restore must
be rehearsed once before launch — a backup that has never been restored is not yet a backup.

## Secrets

Secrets are never committed to the repository.

| Secret                            | Where it lives        | Used by                          |
|-----------------------------------|-----------------------|----------------------------------|
| `JWT_SECRET_KEY`                  | `.env` on the VM only | Server (production)              |
| `DB_PASSWORD`                     | `.env` on the VM only | Server and database (production) |
| Email-delivery credentials        | `.env` on the VM only | Server (production)              |
| `DOCKER_HUB_TOKEN`                | GitHub Secret         | Release workflow                 |
| VM SSH deploy key                 | GitHub Secret         | Release workflow (server)        |
| Android signing key and passwords | GitHub Secret         | Release workflow (Android)       |
| Play Console service-account key  | GitHub Secret         | Release workflow (Android)       |

Production application secrets (`JWT_SECRET_KEY`, `DB_PASSWORD`, email credentials) are deliberately **not** GitHub
Secrets: the workflow only ships an image, and the running container reads its configuration from the VM. That keeps the
production signing key out of CI entirely.

For local development, secrets live in `.env` (gitignored).
See [Security](../security/Security.md) for details on the JWT key requirements.

# PlaneR Deployment

This document describes how PlaneR is built and deployed.
Some sections describe the current plan; others are marked as **TBD** because
the corresponding pipelines or infrastructure do not exist yet.

- [Overview](#overview)
- [Environments](#environments)
- [Server Deployment](#server-deployment)
- [Desktop Deployment](#desktop-deployment)
- [Android Deployment](#android-deployment)
- [TLS and Reverse Proxy](#tls-and-reverse-proxy)
- [Backups](#backups)
- [Secrets](#secrets)

## Overview

PlaneR has three deployable artifacts:

| Artifact | Where it runs | Distribution |
|---|---|---|
| Server | Cloud VM / container host | Docker Hub image |
| Desktop | User's machine | GitHub Releases (MSI, DMG, DEB) |
| Android | User's phone | Google Play Console (internal testing) |

Each is built and published by its own GitHub Actions workflow.

## Environments

| Environment | Purpose | Database |
|---|---|---|
| **Development** | Local development | Docker, or local Postgres |
| **Testing** | CI runs unit and integration tests | Testcontainers (ephemeral) |
| **Production** | Real users | Managed Postgres on the cloud VM |

There is no staging environment. For a personal project, this is acceptable —
the tests provide enough coverage that a broken deploy is caught before it ships.

## Server Deployment

The server is deployed as a **Docker image** built and pushed automatically.

**Pipeline (planned):**

1. A pull request is merged from `develop` into `master`.
2. A GitHub Action builds the server JAR with `./gradlew :planer-server:buildFatJar`.
3. The JAR is packaged into a Docker image based on `eclipse-temurin:21-jre`.
4. The image is tagged with the git SHA and `latest`, then pushed to Docker Hub.
5. The cloud server pulls the new image and restarts the container.
6. On startup, the container runs Flyway migrations automatically before serving.

**Manual override:**

```sh
docker compose -f docker-compose.prod.yml up -d --pull always
```

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
versions manually. Auto-update can be added later via `org.beryx.jlink` or a
similar tool.

## Android Deployment

The Android app is published to the **Google Play Console** on the internal
testing track.

**Pipeline (planned):**

1. A version tag is pushed.
2. A GitHub Action builds a signed AAB with `./gradlew :planer-android:bundleRelease`.
3. The AAB is uploaded to the Play Console via the `r0adkll/upload-google-play` action.
4. Testers receive an update notification.

Signing keys are stored as GitHub Secrets and never committed.

## TLS and Reverse Proxy

The server itself listens on plain HTTP inside the container network.
TLS is terminated by a reverse proxy running on the same host.

**Proposed setup:** **Caddy**, because it handles Let's Encrypt certificate
issuance and renewal automatically with no configuration.

```
planer.example.com {
    reverse_proxy localhost:8080
}
```

That is the entire Caddyfile. Caddy obtains and renews the certificate on its own.

If Caddy is not used, **Nginx** with **Certbot** is the standard alternative.

## Backups

**TBD.** The database will need a backup strategy before there is real user data.
The likely approach is a nightly `pg_dump` piped to **restic** and uploaded to
S3-compatible object storage. Retention: 7 daily, 4 weekly, 12 monthly.

## Secrets

Secrets are never committed to the repository.

| Secret | Where it lives | Used by |
|---|---|---|
| `JWT_SECRET_KEY` | GitHub Secret + `.env` | Server |
| `DB_PASSWORD` | GitHub Secret + `.env` | Server |
| `DOCKER_HUB_TOKEN` | GitHub Secret | CI |
| Android signing key | GitHub Secret | CI (android) |

For local development, secrets live in `.env` (gitignored).
See [Security](../security/Security.md) for details on the JWT key requirements.
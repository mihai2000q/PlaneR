# PlaneR Git

This document describes the conventions used for Git on the PlaneR repository.

- [Git Issues](#git-issues)
- [Git Branches](#git-branches)
- [Git Commits](#git-commits)
- [Pull Requests](#pull-requests)
- [Releases](#releases)

## Git Issues

Usually, whenever the code has to be modified, an issue will be created on **GitHub**.
The title of the issue will be the developer's choice
(for preference, it should resemble the git commits to a higher level overview without,
of course, the issue tag), however, the labels attached to it should be significant.
The title should also include the **module** that is being worked on.

The expected labels to use are:

### Module

- **core** if the shared core module is the topic of the issue
- **server** if the server is the topic of the issue
- **desktop** if the desktop is the topic of the issue
- **android** if the Android client is the topic of the issue
- **docs** if the issue only touches documentation (`.docs/`, READMEs)
- **infra** if the issue touches Docker, GitHub Actions, shell scripts, or the license files

### Type

- **build** if the issue is supposed to update the dependencies
- **bug** if the issue is supposed to solve a known problem
- **code-quality** when the code is refactored for better readability (interchangeable with **refactor**)
- **devOps** when the changes are directed to operations outside the code, i.e., Docker, GitHub Actions
- **documentation** if the source code is not the target, but its documentation
- **feature** for new features that are added to the application
- **performance** when the code is being optimized
- **refactor** when the code is just being cleaned up (interchangeable with **code-quality**)
- **release** for version bumps, changelog updates, and release prep
- **security** for authentication, authorization, or vulnerability fixes
- **styling** when only the style is being affected (usually used for the front end components)
- **test** if a unit or integration test for the source code is being written

An issue typically carries **one module label and one type label**. If a change
spans two modules, prefer opening two issues and referring between them — that
keeps the history clean and avoids branches that touch everything at once.

## Git Branches

Every branch name starts with the **module**, then the **type**, then the
**issue number** and a short description:

```
<module>/<type>/<issue-number>-<short-description>
```

### Module

- **core**
- **server**
- **desktop**
- **android**
- **docs**
- **infra**

### Type

- **build**
- **bug**
- **code** — for _code-quality_ issues
- **devOps**
- **doc** — for _documentation_ issues
- **feature**
- **perform** — for _performance_ issues
- **refactor**
- **release**
- **security**
- **style** — for _styling_ issues
- **test**

### Main branches

- **develop** is the main development branch. Every feature, bug, refactor, and documentation branch is based on it and merged back into it.
- **master** is the release branch. It only receives merges from `develop` via a release PR.

### Examples

If your ticket is "\[Server\] Events - Add CRUD Endpoints" and it is a feature issue:

```
server/feature/12-events-crud-endpoints
```

A bug fix for login on the Android client:

```
android/bug/27-login-token-expiry
```

A documentation issue clarifying the Git conventions themselves:

```
docs/doc/18-clarify-branch-naming
```

An infrastructure issue pinning the Postgres image:

```
infra/devOps/8-pin-postgres-image
```

## Git Commits

The commits on branches should follow a structure (similar to the issues)
(*#TAG MODULE - LAYER/SCREEN: COMPONENT - CHANGES*).
First, there should be a hashtag followed by the number of the issue (e.g., #3)
followed by the module's name that is the target of the commit (e.g., server, android)
followed by a dash and the changes done on it (or the component that was modified, dash, then the changes)
(by preference, try to include a meaningful verb like add or update).

For example:

- `#3 Server - Data - Add Event Repository`
- `#3 Android - Calendar Screen: Event Card - Add Long Press Gesture`
- `#7 Docs - Architecture - Clarify Health Check Endpoint`
- `#12 Infra - Docker - Pin Postgres Image Version`

Note: if the target of the commit has nothing to do with any layer or screen,
no worries, just try to be as concise as possible.
However, if multiple layers or modules are affected, try submitting smaller commits.

## Pull Requests

- **Every PR targets `develop`.**
- **A PR should reference its issue** in the description using `Closes #<issue>` so the issue closes automatically on merge.
- **Merge commits are used.** History is preserved as-is; individual commits on the branch remain visible in the log.
- **Rebase before opening the PR** if your branch has fallen behind `develop` and you want a clean linear history. Once the PR is open, further updates use normal merge commits.

## Releases

Releases are made from `master` and tagged with semantic versioning:

```
v<major>.<minor>.<patch>
```

- **major** — incompatible changes to the domain or API that require clients to update in lockstep
- **minor** — new features that are backward compatible
- **patch** — bug fixes and small improvements

A release is created when a `develop` → `master` PR is merged. The tag is created on the merge commit, and a GitHub Release is published with a short summary of what changed.

The version number is stored in the root `build.gradle.kts` and is the single source of truth for both the server and the clients.
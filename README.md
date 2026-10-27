<!-- krizaka-header -->
<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/krizaka/.github/main/profile/assets/orazaka-dark.svg">
  <img src="https://raw.githubusercontent.com/krizaka/.github/main/profile/assets/orazaka-light.svg" alt="Orazaka" width="96">
</picture>

# Orazaka Knowledge Service

**The AI that never leaves home.**

Knowledge & RAG retrieval service (pgvector) with asynchronous indexing.

[![CI](https://github.com/krizaka/orazaka-knowledge-service/actions/workflows/ci.yml/badge.svg)](https://github.com/krizaka/orazaka-knowledge-service/actions/workflows/ci.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/krizaka/orazaka-knowledge-service/badge)](https://scorecard.dev/viewer/?uri=github.com/krizaka/orazaka-knowledge-service)
[![License: FSL-1.1-ALv2](https://img.shields.io/badge/license-FSL--1.1--ALv2-blue.svg)](LICENSE.md)
[![Orazaka](https://img.shields.io/badge/part%20of-Orazaka-f67e23)](https://github.com/krizaka/orazaka#repositories)
[![Docs](https://img.shields.io/badge/docs-krizaka.com-f67e23)](https://www.krizaka.com/en/products/orazaka)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=krizaka_orazaka-knowledge-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=krizaka_orazaka-knowledge-service) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=krizaka_orazaka-knowledge-service&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=krizaka_orazaka-knowledge-service) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=krizaka_orazaka-knowledge-service&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=krizaka_orazaka-knowledge-service) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=krizaka_orazaka-knowledge-service&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=krizaka_orazaka-knowledge-service) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=krizaka_orazaka-knowledge-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=krizaka_orazaka-knowledge-service)

[Documentation](https://www.krizaka.com/en/products/orazaka) · [Website](https://www.krizaka.com) · [Krizaka on GitHub](https://github.com/krizaka)

</div>
<!-- /krizaka-header -->

**Layer:** Orazaka AI engine · **Version:** `1.0.0-SNAPSHOT` · **License:** FSL-1.1-ALv2 ·
part of the [Orazaka platform](https://github.com/krizaka/orazaka) by [Krizaka](https://krizaka.com)

## What it provides

Retrieval over pgvector (port `8084`): `/internal/v1/knowledge/retrieve`,
`/internal/v1/knowledge/sources/search`, and AMQP-driven RAG indexing. Own database
`orazaka_knowledge_db` (`infra/initdb/40-knowledge.sql`).

## Position in the platform

| | |
|:---|:---|
| Depends on | [`orazaka-build`](https://github.com/krizaka/orazaka-build) |
| Used by | _no other Orazaka repository._ |
| Workspace path | `orazaka-apps/services/orazaka-knowledge-service` |

## Build

**Inside the Orazaka workspace** (recommended — every dependency is built from source):

```bash
git clone https://github.com/krizaka/orazaka.git && cd orazaka
node scripts/workspace.mjs clone          # clones every repository at its workspace path
./mvnw -f orazaka-apps/services/orazaka-knowledge-service/pom.xml verify
```

**Standalone** — upstream artifacts must be in `~/.m2` (built by the workspace) or resolvable from
GitHub Packages (`https://maven.pkg.github.com/krizaka/<repository>`, see the
[workspace README](https://github.com/krizaka/orazaka#consuming-packages)):

```bash
./mvnw verify
```

Requirements: JDK 21, Docker (Testcontainers integration tests).

## Governance

This repository follows the Orazaka governance contract — [AGENTS.md](https://github.com/krizaka/orazaka/blob/main/AGENTS.md)
in the workspace is normative; the local [AGENTS.md](AGENTS.md) only scopes it to this repository.

## CI

The pipeline is the organisation's reusable [`java-ci.yml`](https://github.com/krizaka/.github/blob/main/.github/workflows/java-ci.yml): this service is verified inside the Orazaka workspace against its upstreams built from source. A change that touches only documentation skips the build and still reports green.

## License

[FSL-1.1-ALv2](LICENSE.md) (Functional Source License, Apache-2.0 future licence) © Technologies Krizaka Inc. — read,
run, modify and self-host it for any purpose except offering a competing service; each version becomes Apache-2.0 two
years after its release. Commits before the licence change remain [Apache-2.0](LICENSE-APACHE) — see [NOTICE](NOTICE)
and [ADR-011](https://github.com/krizaka/.github/blob/main/docs/adr/ADR-011-licensing-libraries-open-products-fair-source.md).
The Krizaka names and birds are not licensed: [trademark policy](https://github.com/krizaka/.github/blob/main/TRADEMARKS.md).

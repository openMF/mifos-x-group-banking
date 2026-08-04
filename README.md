<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/branding/logo_dark.svg">
  <img src="docs/assets/branding/logo.svg" alt="MifosSave — five members in a ring around a shared fund" width="150" />
</picture>

<h1>MifosSave</h1>

<p>Offline-first community banking for VSLA / ROSCA / SHG groups — built on Mifos Fineract.</p>

![Kotlin](https://img.shields.io/badge/Kotlin-7f52ff?style=flat-square&logo=kotlin&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin%20Multiplatform-4c8d3f?style=flat-square&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Jetpack%20Compose%20Multiplatform-000000?style=flat-square&logo=android&logoColor=white)
![Mifos Fineract](https://img.shields.io/badge/Mifos%20Fineract-000000?style=flat-square&logo=apache&logoColor=white)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![License](https://img.shields.io/github/license/openMF/mifos-x-group-banking.svg?style=flat-square)](LICENSE)
[![Slack](https://img.shields.io/badge/Slack-4A154B?style=flat-square&logo=slack&logoColor=white)](https://join.slack.com/t/mifos/shared_invite/zt-2wvi9t82t-DuSBdqdQVOY9fsqsLjkKPA)

</div>

> **MifosSave** digitizes the full lifecycle of self-funded community banking groups — meetings, savings, lending, and share-out — with offline-first KMP and a Mifos Fineract backend.

---

## 🌍 The Problem

Millions of people in emerging markets organise into community savings groups — **VSLA** (Village Savings & Loan Associations), **ROSCA** (Rotating Savings & Credit Associations), **ASCA**, and **SHG** (Self-Help Groups) — to pool savings, issue loans, and distribute profits.

These groups operate with paper ledgers, leading to:

- Record-keeping errors and disputes
- Fraud and limited transparency
- Inability to scale beyond a single notebook
- No integration with formal financial systems

Existing digital solutions are either **online-only**, lack **Fineract integration**, or don't support the **full group lifecycle** end-to-end.

## ✨ The Solution

A Kotlin Multiplatform mobile app that digitises every step of the community banking lifecycle — from group creation and member onboarding, through meeting-driven savings collection and loan disbursement, to periodic share-out — all working **offline-first** with automatic sync to Mifos Fineract when connectivity returns.

### Why MifosSave

| | |
|---|---|
| 📵 **Offline-first** | Full functionality without internet — sync when connected (target: 95% sync success on 2G/3G) |
| 🤝 **Meeting-centric workflow** | Mirrors how real groups operate: attendance → savings → loans → decisions |
| 🏦 **Fineract-backed** | Enterprise-grade financial record-keeping via proven open-source core banking |
| 👁️ **Low-literacy UX** | Icon-heavy, minimal text, voice prompts, large touch targets (≥48dp) |
| 🔍 **Transparent** | Every member sees group balances, loan status, and share-out projections in real-time |

---

## 👥 Who It's For

MifosSave ships as **two client surfaces** in a single codebase:

### 🛠 Admin Client (staff auth)

| Persona | Role | Key Capabilities |
|---------|------|------------------|
| **Amina** | Treasurer / Secretary | Record savings (group-linked + individual), track loans, calculate share-out, mark attendance |
| **Joseph** | Chairperson | Conduct meetings, approve/reject loans, initiate share-out, manage group parameters |
| **David** | MFI Field Officer | Read-only cross-group monitoring, force sync, generate reports |
| **Sarah** | NGO Program Manager | Read-only analytics, donor reports, programme metrics |

### 📱 End User Client (self-service auth)

| Persona | Role | Key Capabilities |
|---------|------|------------------|
| **Grace** | Regular Group Member | View own savings/loans, group summary, request loans, see share-out projection |

> Two client types, two navigation graphs, two permission models — one app.

---

## 🎯 Feature Roadmap

**v1.0.0 — Core Group Banking** (13 features)

| # | Feature | Client | Description |
|---|---------|--------|-------------|
| 1 | Authentication | both | Fineract credentials + local PIN + optional biometric for offline |
| 2 | End-User Dashboard | end user | Personal savings, loans, request submission |
| 3 | Group Management | admin | Create, configure, monitor savings groups |
| 4 | Member Onboarding | admin | Register members with name, photo, phone, role |
| 5 | Meeting Lifecycle | admin | Schedule, conduct, summarise meetings; review previous |
| 6 | Savings Collection | admin | Validated contribution recording during meetings |
| 7 | Group-Linked Savings | admin | Mandatory min/max-enforced group savings (CR-003) |
| 8 | Corpus Tracking | admin | Real-time fund balance with excess outflow blocking |
| 9 | Loan Management | admin | Application → vote-based approval → disbursement → repayment |
| 10 | Share-Out | admin | End-of-cycle calculation + distribution |
| 11 | Offline Sync | both | SQLDelight cache + queue-based Fineract batch sync |
| 12 | Fines Tracking | admin | Late attendance, missed meetings, late repayment |
| 13 | Multi-Language | both | English, Swahili, French, Hindi (runtime switch) |

**v1.1.0 — Supervision & Social**: Field-officer view, social fund.
**v2.0.0 — Scale & Integrate**: Mobile money, web admin, SMS, inter-group lending, credit scoring.

> Full feature matrix, screen inventory (28 screens), and acceptance criteria live in the `idea-layer/` of the parent product-cycle repository.

---

## 📊 Success Metrics

- 50+ active groups onboarded within 6 months of launch
- 90% reduction in meeting duration vs paper-based workflow
- Zero record-keeping disputes in digitised groups
- 95% sync success rate on 2G/3G connections
- < 5 minute onboarding time for new group treasurer
- 100% data parity between local cache and Fineract after sync

---

## 🏗️ Architecture

| Concern | Choice |
|---------|--------|
| **Language** | Kotlin |
| **UI** | Compose Multiplatform |
| **Architecture** | Clean Architecture + MVI / UDF |
| **DI** | Koin |
| **Navigation** | Voyager |
| **Network** | Ktor + Ktorfit |
| **Local DB** | Room KMP / SQLDelight (offline-first) |
| **Backend** | Mifos Fineract (REST + 36 generated MCP tools) |
| **Platforms** | Android, iOS *(desktop & web intentionally out — rural mobile-only use-case)* |

### Module Layout

```
cmp-android      — Android entry point + platform actuals
cmp-ios          — iOS entry point + platform actuals
cmp-shared       — App composition root (shared across platforms)
cmp-navigation   — Voyager screen graph (admin + end-user)
core/            — Common modules: domain, data, network, designsystem, ui
core-base/       — Foundation: model, network, store, common
feature/         — Feature implementations (per FEATURES.md matrix)
build-logic/     — Convention plugins
fastlane/        — iOS deployment automation
```

### Data Model

8 core entities mapped to Fineract primitives + 10 custom Data Tables for group-banking domain extensions:

| Entity | Fineract Mapping |
|--------|------------------|
| Group | `m_center` + `dt_group_config` |
| Member | `m_client` + `dt_member_role` |
| Meeting | `dt_meeting_record` + `dt_meeting_attendance` |
| Savings Transaction | `m_savings_account_transaction` |
| Loan | `m_loan` + `dt_loan_vote` |
| Loan Repayment | `m_loan_transaction` |
| Sync Queue | local-only (offline-first) |

> Custom Data Tables: `dt_group_config`, `dt_meeting_record`, `dt_meeting_attendance`, `dt_member_role`, `dt_share_out`, `dt_social_fund`, `dt_loan_vote`, `dt_sync_metadata`, `dt_loan_request`, `dt_group_corpus`. Schemas in `server-layer/API_CONTRACT.yaml`.

---

## 🚀 Getting Started

### Prerequisites

- JDK 17+
- Android Studio (Hedgehog or newer) / IntelliJ IDEA
- Xcode 15+ (for iOS development)
- Kotlin Multiplatform Mobile plugin

### Clone & Build

```bash
git clone https://github.com/openMF/mifos-x-group-banking.git
cd mifos-x-group-banking
./gradlew build
```

### Run

```bash
# Android
./gradlew :cmp-android:installDebug

# iOS — open via Xcode
open cmp-ios/iosApp/iosApp.xcodeproj
```

### Backend Configuration

MifosSave talks to **Mifos Fineract**. The default sandbox is configured for development:

```
Base URL: https://sandbox.mifos.community/fineract-provider/api/v1
Tenant:   default
```

For production deployment, point to your own Fineract instance — see [`docs/`](docs/).

---

## 🍎 iOS Deployment

The project ships production-ready iOS deployment via Firebase App Distribution, TestFlight, and App Store Connect.

```bash
# One-time setup
bash scripts/setup_ios_complete.sh

# Deploy
bash scripts/deploy_firebase.sh    # Internal QA
bash scripts/deploy_testflight.sh  # Beta
bash scripts/deploy_appstore.sh    # Production
```

Configuration uses a shared-vs-app-specific split:

- **Shared (IOS_SHARED)**: Team ID, API keys, Match repo
- **App-specific (IOS)**: Bundle ID, Firebase app ID

See:
- [iOS Setup Guide](docs/IOS_SETUP.md)
- [iOS Deployment Guide](docs/IOS_DEPLOYMENT.md)
- [GitHub Actions iOS Configuration](docs/GITHUB_ACTIONS_IOS_MIGRATION.md)

---

## 🎨 Brand

| Element | Value |
|---------|-------|
| Display name | **MifosSave** |
| Primary | `#2E7D32` — Forest Green (growth, trust; WCAG AAA on white) |
| Accent | `#FF8F00` — Amber |
| Typography | Noto Sans (large scale for low-literacy users) |
| Iconography | Rounded |
| Touch target | ≥ 48dp (accessibility floor) |

---

## 🧭 Repository Layout

This repo holds the **source code** for MifosSave. It was scaffolded from [openMF/kmp-project-template](https://github.com/openMF/kmp-project-template) and customised for the group-banking domain (`org.mifos.groupbanking`).

The full product cycle (idea, server contract, design tokens, feature plan, implementation pipeline) lives in the parent **claude-product-cycle** workspace under `workspaces/mifos-x/mifos-x-group-banking/`.

---

## 📚 Documentation

- [Setup Guide](docs/SETUP.md)
- [Architecture Overview](docs/ARCHITECTURE.md)
- [Source Set Hierarchy](docs/SOURCE_SET_HIERARCHY.md)
- [Style Guide](docs/STYLE_GUIDE.md)
- [Sync with Template](docs/SYNC_SCRIPT.md)
- [Secrets Manager](docs/SECRETS_MANAGER.md)
- [Fastlane Configuration](docs/FASTLANE_CONFIGURATION.md)

---

## 🤝 Contributing

PRs welcome — please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) first.

```bash
# Standard flow
git checkout -b feat/your-feature
# ... make changes ...
git commit -m "feat(scope): your change"
git push origin feat/your-feature
# Open PR against openMF/mifos-x-group-banking
```

---

## 📫 Support & Community

- 💬 [Mifos Slack](https://join.slack.com/t/mifos/shared_invite/zt-2wvi9t82t-DuSBdqdQVOY9fsqsLjkKPA)
- 🐛 [Issue Tracker](https://github.com/openMF/mifos-x-group-banking/issues)
- 📋 [Jira (KMPPT)](https://mifosforge.jira.com/jira/software/c/projects/KMPPT/boards/63)

---

## 📄 License

[Mozilla Public License 2.0](LICENSE)

> Built on [openMF/kmp-project-template](https://github.com/openMF/kmp-project-template) — sync upstream improvements via `customizer.sh`.

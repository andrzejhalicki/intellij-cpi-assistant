# IntelliJ Plugin: CPI Assistant

[![Version](https://img.shields.io/jetbrains/plugin/v/25099-cpi-assistant)](https://plugins.jetbrains.com/plugin/25099-cpi-assistant)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25099-cpi-assistant)](https://plugins.jetbrains.com/plugin/25099-cpi-assistant)
[![License](https://img.shields.io/github/license/andrzejhalicki/intellij-cpi-assistant)](https://github.com/andrzejhalicki)

## Overview

The plugin enables modification and deployment SAP Cloud Platform Integration Groovy scripts directly within the IDE.<br>

- Update SAP CPI Groovy scripts directly in IntelliJ</li>
- Deploy artifacts to SAP CPI without leaving the IDE</li>
- Integrated project structure for easy file management</li>
- User-friendly interface for efficient CPI development</li>

## Installation

You can install the plugin directly from the JetBrains Marketplace:

1. Go to `Settings` > `Plugins` > `Marketplace`.
2. Search for `CPI Assistant`.
3. Click `Install`.

Alternatively, you can install it from a local JAR file:

1. Download the latest release from the [Releases](https://github.com/andrzejhalicki/intellij-cpi-assistant/releases) page.
2. Go to `Settings` > `Plugins` > `Install Plugin from Disk`.
3. Select the downloaded JAR file.

## CPI Tenant setup

After installing, you can find the plugin in the toolbar.

<img src=".github/images/CPIA-1.jpg" alt="Plugin Screenshot" width="200">

In order to add a new tenant click the `+` button.

<img src=".github/images/CPIA-2.jpg" alt="Plugin Screenshot" width="200">

You will see a window to paste service key and name.

<img src=".github/images/CPIA-3.jpg" alt="Plugin Screenshot" width="200">

> **Hint:** SAP Process Integration Runtime instance needs to have roles `WorkspaceArtifactsDeploy`,`AuthGroup_IntegrationDeveloper` assigned.

After clicking `OK` button the new tenant will start loading packages, artifacts and scripts.

<img src=".github/images/CPIA-4.jpg" alt="Plugin Screenshot" width="200">

### Mapping/Adding script

For already existing scripts in the tenant you need to map local script first.
Right click on script and choose `Map script`.

<img src=".github/images/CPIA-map-script.jpg" alt="Plugin Screenshot" width="200">

After choosing your script you will see a path next to the script name. Now you are ready to update it in the tenant. Right click on the script again and choose `Update in tenant`.

<img src=".github/images/CPIA-update-in-tenant.jpg" alt="Plugin Screenshot" width="300">

> **Hint:** Make sure the artifact is not locked before updating and deploying from the tool.

### Flow/Script collection deployment

When you have finished with the script update you can now deploy the flow or script collection. Right click on the artifact and choose `Deploy`.

<img src=".github/images/CPIA-deploy.jpg" alt="Plugin Screenshot" width="200">

It's also possible to add a new script directly under a flow or script collection with button `Add script`.

## Development & CI/CD

### Continuous Integration

This project uses GitHub Actions for automated testing and quality assurance:

- **🧪 Unit Tests**: Automatically run on every PR and commit to main
- **🔨 Build Validation**: Ensures the plugin compiles correctly
- **📊 Test Reports**: Detailed test results and coverage tracking
- **🤖 Automated PR Creation**: Auto-generated PRs for feature branches

#### CI Workflow Status

[![CI](https://github.com/andrzejhalicki/CPIAssistant/actions/workflows/ci.yml/badge.svg)](https://github.com/andrzejhalicki/CPIAssistant/actions/workflows/ci.yml)

#### Running Tests Locally

```bash
# Run all unit tests
./gradlew test

# Build the project
./gradlew build

# Clean build
./gradlew clean build
```

#### Quality Gates

All pull requests must pass:
- ✅ Unit tests (14 comprehensive tests)
- ✅ Build validation
- ✅ Code review (automated via Claude)

#### Test Coverage

Current test coverage focuses on critical components:
- **CpiService**: 95% coverage (authentication, API calls, deployment)
- **Overall Project**: ~15% coverage with comprehensive critical path testing

See `TEST_COVERAGE.md` for detailed coverage tracking and roadmap.

### Contributing

1. Create a feature branch: `git checkout -b feature/your-feature-name`
2. Make your changes and add tests
3. Ensure tests pass: `./gradlew test`
4. Push your branch - CI will automatically create a PR
5. Wait for automated code review and CI checks
6. Address any feedback and merge when approved


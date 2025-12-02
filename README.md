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

## Version 1.2.0

### Artifact refresh and sync of multiple scripts

<img src=".github/images/CPI-refresh.jpg" alt="Plugin Screenshot" width="300">

You can now refresh the artifact and sync multiple scripts at once. Right click on the artifact and choose `Refresh`. The plugin will check all scripts under the artifact and if there are any changes in the tenant it will refresh the tree.
If there are multiple scripts in one artifact you can download them all at once. Right click on the artifact and choose `Sync all scripts`.

### Favorites
<img src=".github/images/CPI-favorites.jpg" alt="Plugin Screenshot" width="300">

You can now mark your favorite packages with a star icon. Right click on the script and choose `Add to Favorites`. You can view all your favorite scripts in the `Favorites` node. These packages will be loaded first.

### Improved status tracking of deployment and script update
<img src=".github/images/CPI-deploy-status.jpg" alt="Plugin Screenshot" width="300">

Progress bar will now show the status of deployment and script update.

## Version 1.3.0
### Lazy loading of artifacts and resources
Only packages are loaded on startup. Artifacts and resources are loaded when expanded. Exception is made for favorite packages which are loaded on startup in full scope.
### Added support for XSLT
<img src=".github/images/XSLT.jpg" alt="Plugin Screenshot" width="300">

### Download and map single resource
You can now download and map single resource from the tenant. Right click on the resource and choose `Download From Tenant`.

<img src=".github/images/download-from-tenant.png" alt="Plugin Screenshot" width="300">

### Favorites support for artifacts and resources. Favorite objects are loaded automatically.
You can now mark your favorite artifacts and resources with a star icon. These will be loaded first. If you mark a whole package or artifact, the whole content will be loaded on startup.

Right click on the artifact/resource and choose `Add to Favorites`.

> **Hint:** You may need to delete favoritePackages node from .idea/TenantState.xml as structure of the file changed.
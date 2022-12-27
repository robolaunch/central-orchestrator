# <img src="https://raw.githubusercontent.com/robolaunch/trademark/main/logos/svg/rocket.svg" width="40" height="40" align="top"> Central Orchestrator

Central Orchestrator collects all other robolaunch components under the same roof. Makes robolaunch accessible and usable by the end users.

<div align="center">
  <p align="center">
    <a href="https://github.com/robolaunch/template/releases">
      <img src="https://img.shields.io/badge/Java-11-orange" alt="release">
    </a>
    <a href="https://github.com/robolaunch/central-orchestrator/releases">
      <img src="https://img.shields.io/badge/release-v0.0.1-green" alt="release">
    </a>
    <a href="https://github.com/robolaunch/central-orchestrator/blob/main/LICENSE">
      <img src="https://img.shields.io/github/license/robolaunch/central-orchestrator" alt="license">
    </a>
    <a href="https://github.com/robolaunch/central-orchestrator/issues">
      <img src="https://img.shields.io/github/issues/robolaunch/central-orchestrator" alt="issues">
    </a>
    <a href="https://github.com/robolaunch/central-orchestrator/actions">
      <img src="https://img.shields.io/badge/build-passing-dgreen" alt="build">
    </a>
  </p>
</div>

Central Orchestrator integrates other robolaunch components, and generates an end user consumable endpoints.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
  - [Installation](#installation)
  - [Deploy Your First Robot](#deploy-your-first-robot)
- [Aims & Roadmap](#aims--roadmap)
- [Contributing](#contributing)


## Overview

The aim of this project is to make robolaunch accessible and usable for everyone.

- Kubernetes Integration
- User Management
- Resource Management
- Observability & Monitoring
- Robot Management

## Quick Start

After cloning this repository:
1. Build the application with Maven:
```
mvn clean install
```
2. Install and configure kogito components using [this docker-compose file](https://github.com/kiegroup/kogito-examples/blob/stable/kogito-quarkus-examples/process-usertasks-timer-quarkus-with-console/docker-compose/docker-compose-infinispan.yml).

3. Start the Central Orchestrator in development mode:
```
mvn quarkus:dev
```

## Aims & Roadmap

- Reducing the entry barrier for robotics
- Make robotics development and deployment easier and faster.

## Contributing

Please see [this guide](./CONTRIBUTING) if you want to contribute.

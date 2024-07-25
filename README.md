Keycloak SMS Authenticator
===

This project provides the [Keycloak SMS authenticator](./extensions/keycloak-sms-authenticator/README.md) extension.

The following submodules have the artifact deployment to the maven repository skipped in their pom.xml:

- config
- container
- docker-compose
- helm
- server
- themes

The above submodules are only used during the development of the extension.

Project Template
---

This project is based on the [custom Keycloak template](https://github.com/inventage/keycloak-custom). It is structured as a multi-module Maven build and contains the following top-level modules:

- `config`: provides the build stage configuration and the setup of Keycloak
- `container`: creates the custom docker image
- `docker-compose`: provides a sample for launching the custom docker image
- `extensions`: provides samples for Keycloak SPI implementations
- `helm`: provides a sample for installing the custom container image in Kubernetes using the Codecentric Helm Chart
- `server`: provides a Keycloak installation for local development & testing
- `themes`: provides samples for custom themes

Please see the tutorial [building a custom Keycloak container image](https://keycloak.ch/keycloak-tutorials/tutorial-custom-keycloak/) for the details of this project.

[Keycloak]: https://keycloak.org


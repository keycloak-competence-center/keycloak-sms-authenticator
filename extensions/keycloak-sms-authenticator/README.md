Keycloak SMS Authenticator
===

This Keycloak extension allows to use a code sent by SMS as a second factor.

To install the SMS Authenticator add the jar to the Keycloak server:

```shell
cp target/keycloak-sms-authenticator-1.0.0-SNAPSHOT.jar _KEYCLOAK_HOME_/providers/
```

Configuration
---

```json
{
  "authenticationFlows": [
    {
      "alias": "browser-sms Browser - Conditional SMS",
      "description": "Flow to determine if the SMS is required for the authentication",
      "providerId": "basic-flow",
      "topLevel": false,
      "builtIn": false,
      "authenticationExecutions": [
        {
          "authenticator": "conditional-user-configured",
          "authenticatorFlow": false,
          "requirement": "REQUIRED",
          "priority": 10,
          "autheticatorFlow": false,
          "userSetupAllowed": false
        },
        {
          "authenticator": "sms-authenticator",
          "authenticatorConfig": "sms-authenticator",
          "authenticatorFlow": false,
          "requirement": "REQUIRED",
          "priority": 20,
          "autheticatorFlow": false,
          "userSetupAllowed": false
        }
      ]
    }
  ],
  "authenticatorConfig": [
    {
      "alias": "sms-authenticator",
      "config": {
        "sms-service-provider-id": "uniport-sms-service",
        "sms-code-ttl": "60",
        "sms-code-length": "4"
      }
    }
  ]
}
```

```json
{
  "requiredActions": [
    {
      "alias": "sms-config",
      "name": "Configure SMS",
      "providerId": "sms-config",
      "enabled": true,
      "defaultAction": false,
      "priority": 1001,
      "config": {
        "sms-service-provider-id": "sms-to-console",
        "sms-code-ttl": "60",
        "sms-code-length": "5"
      }
    }
  ]

}
```
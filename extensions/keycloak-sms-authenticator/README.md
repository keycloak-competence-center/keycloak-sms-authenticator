Keycloak SMS Authenticator
===

The keycloak-sms-authenticator extension allows to use a code sent by SMS as a second factor.

To install the SMS Authenticator add the jar to the Keycloak server:

```shell
cp target/keycloak-sms-authenticator-<VERSION>.jar <KEYCLOAK_HOME>/providers/
```

Gateways
---

Following SMS Gateways implementations are supported:

| Name                        |                Provider Id                 | Description                                                   |
|-----------------------------|:------------------------------------------:|---------------------------------------------------------------|
| SMS to console |              `sms-to-console`              | Writes SMS Code to console                                    |
|        Uniport SMS        | `uniport-sms-service` | Sends SMS request to [Uniport](https://uniport.ch)            |
|      SMS Factor          |                 `sms-factor`                 | Sends SMS request to [SMS Factor](https://www.smsfactor.com/) |

Configuration
---

The following snippets belong all to the same JSON file

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
                "sms-code-length": "4", 
                "sms-show-phone-number": false
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
        "sms-code-length": "5", 
        "sms-show-phone-number": false
      }
    }
  ]
}
```

```json
{
    "components": {
        "com.inventage.keycloak.sms.gateway.SmsServiceProvider": [
            {
                "name": "SmsFactorProvider",
                "providerId": "sms-factor",
                "subComponents": {},
                "config": {
                    "service-url": [
                        "https://api.smsfactor.com/send/simulate"
                    ],
                    "sender-address": [
                        "Keycloak"
                    ],
                    "sms-factor-token": [
                        "token"
                    ]
                }
            }
        ]
    }
}
```

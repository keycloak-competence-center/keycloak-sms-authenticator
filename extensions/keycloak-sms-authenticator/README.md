Keycloak SMS Authenticator
===

The keycloak-sms-authenticator extension allows to use a code sent by SMS as a second factor.

To install the SMS Authenticator add the jar to the Keycloak server:

```shell
cp target/keycloak-sms-authenticator-<VERSION>.jar <KEYCLOAK_HOME>/providers/
```

Upon successful installation the authenticator "SMS Authentication" (`sms-authenticator`) and the required action "Configure SMS" (`sms-config`) are available.

Configuration
---

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `sms-service-provider-id` | The SPI provider ID for the SMS gateway | `sms-to-console` |
| `sms-code-ttl` | Code validity in seconds | `300` |
| `sms-code-length` | Number of characters in the generated code | `6` |
| `sms-code-characters` | Allowed characters for code generation. Must be at least 2 characters long to take effect; otherwise falls back to alphanumeric. Set to e.g. `0123456789` for digits only, or `11` to always generate codes consisting of `1`s (useful for testing). | _(empty = alphanumeric)_ |
| `sms-show-phone-number` | Whether to display the phone number on the code entry screen | `false` |
| `sms-phone-number-validation-regex` | Regex to validate phone numbers (empty = accept all). The regex is matched against the cleaned number (see below). Example for Swiss mobile: `^\+417[5-9]\d{7}$` | _(empty)_ |
| `sms-phone-number-validation-hint` | Human-readable hint used as the phone number input placeholder (e.g. `+41 7x xxx xx xx`) | _(empty)_ |

### Phone Number Cleaning

Phone numbers are normalized before validation and storage:
- Whitespace, dashes, and parentheses are stripped
- The international `00` prefix is converted to `+` (e.g. `0041...` → `+41...`)

The validation regex is matched against the cleaned number, so it should be written accordingly (e.g. expect `+41...` rather than `0041...`).

### SMS Resend Behavior

- On first visit, the SMS is sent automatically
- Page refresh does not re-send the SMS
- A "Resend SMS Code" button allows the user to explicitly request a new code

### Rate Limiting

Gateway implementations may throw `SmsRateLimitedException` when the SMS gateway rejects requests due to rate limiting (e.g. HTTP 429). The user sees a localized message asking them to wait before retrying. This exception is part of the `com.inventage.keycloak.sms.gateway` package.

### Brute Force Protection

Invalid SMS code attempts during the login flow (authenticator) trigger Keycloak's brute force protection when configured on the realm. The required action flow (phone number setup) does not trigger brute force, as it is a setup step for already-authenticated users.

### Example: Authentication Flow

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
                "sms-show-phone-number": false,
                "sms-phone-number-validation-regex": "^\\+417[5-9]\\d{7}$",
                "sms-phone-number-validation-hint": "+41 7x xxx xx xx"
            }
        }
    ]
}
```

### Example: Required Action

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
                "sms-show-phone-number": false,
                "sms-phone-number-validation-regex": "^\\+417[5-9]\\d{7}$",
                "sms-phone-number-validation-hint": "+41 7x xxx xx xx"
            }
        }
    ]
}
```

Gateways
---

Following SMS Gateway implementations are supported:

| Name                        |                Provider Id                 | Description                                                   |
|-----------------------------|:------------------------------------------:|---------------------------------------------------------------|
| SMS to console |              `sms-to-console`              | Writes SMS Code to console                                    |
|        Uniport SMS        | `uniport-sms-service` | Sends SMS request to [Uniport](https://uniport.ch)            |
|      SMS Factor          |                 `sms-factor`                 | Sends SMS request to [SMS Factor](https://www.smsfactor.com/) |

The [sms-service](./src/main/java/com/inventage/keycloak/sms/gateway/SmsServiceSpi.java) SPI allows to add addtional custom SMS Gateway implementations.

### SMS to console

### Uniport SMS

https://docs.uniport.ch/introduction/sms/

### SMS Factor

https://www.smsfactor.com/

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

Keycloak SMS Authenticator
===

The keycloak-sms-authenticator extension allows to use a code sent by SMS as a second factor.

To install the SMS Authenticator add the jar to the Keycloak server:

```shell
cp target/keycloak-sms-authenticator-<VERSION>.jar <KEYCLOAK_HOME>/providers/
```

Upon successful installation the authenticator "SMS Authentication" (`sms-authenticator`) and the required actions "Configure SMS" (`sms-config`) and "Verify SMS" (`sms-verify`) are available.

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
| `sms-phone-number-validation-regex` | _(sms-config only)_ Regex to validate phone numbers (empty = accept all). The regex is matched against the cleaned number (see below). Example for Swiss mobile: `^\+417[5-9]\d{7}$` | _(empty)_ |
| `sms-phone-number-validation-hint` | _(sms-config only)_ Human-readable hint used as the phone number input placeholder (e.g. `+41 7x xxx xx xx`) | _(empty)_ |

### Phone Number Cleaning

Phone numbers are normalized before validation and storage:
- Whitespace, dashes, and parentheses are stripped
- The international `00` prefix is converted to `+` (e.g. `0041...` → `+41...`)

The validation regex is matched against the cleaned number, so it should be written accordingly (e.g. expect `+41...` rather than `0041...`).

### Phone Number Entry Screen

The phone number entry template (`keycloak-sms-authenticator_sms-config.ftl`) receives the following attributes:

| Attribute | Description |
|-----------|-------------|
| `username` | The authenticated user's username, for personalized greetings |
| `phoneNumber` | The previously entered phone number, for repopulating the input on validation error |
| `phoneNumberHint` | The configured validation hint, for use as input placeholder |

Phone number validation errors (empty, invalid format) are provided as per-field errors via `messagesPerField` for the field `phone-number`, enabling inline error display and `aria-invalid` support. System-level errors (e.g. SMS sending failure) are set as global messages.

### SMS Code Entry Screen

The SMS code entry template (`keycloak-sms-authenticator_login-sms.ftl`) receives the following attributes:

| Attribute | Description |
|-----------|-------------|
| `realm` | The realm model, used for display name in the title |
| `showPhoneNumber` | Whether to display the phone number (from `sms-show-phone-number` config) |
| `mobileNumber` | The user's phone number, shown when `showPhoneNumber` is true |
| `smsResent` | Set to `true` after a successful resend, to display a confirmation message |
| `resetPhoneNumberUri` | URL to reset the phone number (only present in the required action flow) |

Code validation errors (empty, invalid) are provided as per-field errors via `messagesPerField` for the field `code`, enabling inline error display and `aria-invalid` support. System-level errors (e.g. expired code, rate limited, SMS sending failure) are set as global messages.

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
                "sms-show-phone-number": false
            }
        }
    ]
}
```

### Required Actions

Two required actions are available:

- **Configure SMS** (`sms-config`) — prompts the user to enter a phone number and verify it via SMS code. Creates an SMS credential on success. Use this when users set up their own phone number (e.g. during registration or via account management). Implements `CredentialRegistrator`, so Keycloak's login flow can trigger it automatically when no SMS credential exists.

- **Verify SMS** (`sms-verify`) — sends an SMS code to the user's existing phone number and shows only the code entry screen (no phone number entry). Does not create or modify credentials. Use this for onboarding flows where an admin pre-provisions the phone number via the Keycloak admin API, and the user only needs to prove they control it.

### Example: Required Actions

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
        },
        {
            "alias": "sms-verify",
            "name": "Verify SMS",
            "providerId": "sms-verify",
            "enabled": true,
            "defaultAction": false,
            "priority": 25,
            "config": {
                "sms-service-provider-id": "sms-to-console",
                "sms-code-ttl": "60",
                "sms-code-length": "5",
                "sms-show-phone-number": true
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

<p align="center">
  <img src="https://raw.githubusercontent.com/ktestify/.github/refs/heads/main/profile/assets/png/ktestify-banner-2x.png" alt="ktestify-core" width="100%"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/build-passing-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="build passing"/>
  <img src="https://img.shields.io/badge/license-Apache%202.0-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="license"/>
  <img src="https://img.shields.io/badge/java-25-2DD4BF?style=flat-square&labelColor=0C1018&color=2DD4BF" alt="java 25"/>
  <img src="https://img.shields.io/badge/version-0.0.1--SNAPSHOT-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="version"/>
</p>

<br/>
---

# KTestify Plugin Skeleton

What does the plugin do in a nutshell? Describe the main functionality in 1-2 sentences.



## Getting Started

### Maven dependency

```xml
<dependency>
    <groupId>io.github.ktestify</groupId>
    <artifactId>ktestify-plugin-myplugin</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Configuration

Add to your `application.conf` (or override via environment variables):

```hocon
ktestify.plugins.my-plugin {
  # TODO: document your plugin's config keys here
  my-setting = "value"
  my-setting = ${?KTESTIFY_MYPLUGIN_MY_SETTING}

  read-timeout  = 30s
  read-timeout  = ${?KTESTIFY_MYPLUGIN_READ_TIMEOUT}

  poll-interval = 500ms
}
```

---

## Gherkin Steps

### Background — register a resource

```gherkin
Background:
  Given MyPlugin resource
    | resourceName | resourceAlias | myConnParam |
    | my-resource  | my-alias      | ...         |
```

### Action — produce / upload

```gherkin
When MyPlugin record is sent from file
  | resourceAlias | file         |
  | my-alias      | payload.json |
```

### Validation — assert content

```gherkin
Then expected MyPlugin record from file
  | resourceAlias | file          | readTimeout | excludedKeys |
  | my-alias      | expected.json | 30          | timestamp,id |
```

### Negative assertion

```gherkin
And MyPlugin record should not appear
  | resourceAlias | readTimeout |
  | my-alias      | 10          |
```

---

## Architecture

This plugin follows the three-layer separation defined by ktestify-core:

```
┌──────────────────────────────────────────────────────────────┐
│  TRANSPORT                 │  ORCHESTRATION  │  ASSERTION    │
│  MyPluginRecordFetcher     │  MyPluginConsumer│ RecordMatcher │
│  implements RecordFetcher  │                 │ (from core)   │
└──────────────────────────────────────────────────────────────┘
```

- **Transport** — `MyPluginRecordFetcher` implements `RecordFetcher<String>` from ktestify-core.
  Returns `List<ConsumedRecord<String>>` — the only type crossing layer boundaries.
- **Orchestration** — `MyPluginConsumer` wires fetch → match → result.
- **Assertion** — all standard `RecordMatcher` implementations from ktestify-core are reused as-is.

---

## Development

```bash
# Compile
mvn compile

# Unit tests only (no Docker)
mvn test

# All tests including integration tests (requires Docker)
mvn verify

# Code formatting
mvn spotless:apply
```

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).


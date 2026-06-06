<p align="center">
  <img src="https://raw.githubusercontent.com/ktestify/.github/refs/heads/main/profile/assets/png/ktestify-banner-2x.png" alt="ktestify-plugin-notifications" width="100%"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/build-passing-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="build passing"/>
  <img src="https://img.shields.io/badge/license-Apache%202.0-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="license"/>
  <img src="https://img.shields.io/badge/java-25-2DD4BF?style=flat-square&labelColor=0C1018&color=2DD4BF" alt="java 25"/>
  <img src="https://img.shields.io/badge/version-0.1.0-6EE7B7?style=flat-square&labelColor=0C1018&color=6EE7B7" alt="version"/>
</p>

<br/>

---

# ktestify-plugin-notifications

Cross-platform test suite notifications for ktestify-cucumber. Sends a rich summary card to **Microsoft Teams**, **Slack**, or any **HTTP webhook** at the end of each test run, with per-tag group breakdowns, CI/Git context, configurable success-rate thresholds, and fully user-overridable templates.

---

## Features

- 📣 **Four built-in channels**, Teams (Adaptive Card), Slack (Block Kit), generic HTTP webhook, and a zero-dependency log channel
- 🏷️ **Tag-based scenario grouping**, configurable groups (e.g. `@orders`, `@payments`) each get their own success rate and visual style in the card
- 🤖 **CI/CD auto-detection**, branch, commit, build number and pipeline link are automatically extracted from GitLab CI, GitHub Actions, CircleCI, Jenkins and others (via `cucumber-cienvironment`)
- 🎨 **Configurable thresholds**, success rate drives visual card styling (`good` ≥ 75 %, `warning` ≥ 50 %, `attention` < 50 %)
- 📝 **Fully overridable templates**, use HOCON `"""..."""` inline strings, a classpath resource, or a filesystem path; fall back to bundled defaults
- 🔌 **Extensible via SPI**, custom channels registered in `META-INF/services/…NotificationChannel` are discovered automatically
- ⚡ **Async & failure-safe**, notifications run in a background daemon thread pool; a failing webhook never fails a test
- 🔕 **Zero-overhead when disabled**, the plugin is `enabled = false` by default

---

## Getting Started

### 1, Add the Maven dependency

```xml
<dependency>
    <groupId>io.github.ktestify</groupId>
    <artifactId>ktestify-plugin-notifications</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2, Enable notifications in `application.conf`

The plugin is **disabled by default**. Activate it by setting `enabled = true` and configuring at least one channel:

```hocon
ktestify.plugins.notifications {
  enabled     = true
  enabled     = ${?KTESTIFY_NOTIFICATIONS_ENABLED}

  suite {
    name        = "My Integration Tests"
    environment = ${?KTESTIFY_ENV}        # e.g. "staging"
    report-url  = ${?KTESTIFY_REPORT_URL} # link to Allure / GitLab Pages report
  }

  channels = [
    {
      type        = "teams"
      enabled     = true
      webhook-url = ${?KTESTIFY_TEAMS_WEBHOOK_URL}
    }
  ]
}
```

That's all, the Cucumber hooks are discovered automatically via the `KtestifyPlugin` SPI. No `@RunWith` changes or glue package edits are needed.

---

## Configuration Reference

### Top-level keys

| Key | Type | Default | Env var | Description |
|---|---|---|---|---|
| `enabled` | bool | `false` | `KTESTIFY_NOTIFICATIONS_ENABLED` | Master on/off switch |
| `on-failure-only` | bool | `false` | `KTESTIFY_NOTIFICATIONS_ON_FAILURE_ONLY` | Global default; each channel can override |

### `suite` block

| Key | Default | Env var | Description |
|---|---|---|---|
| `name` | `"ktestify Test Suite"` | `KTESTIFY_NOTIFICATIONS_SUITE_NAME` | Displayed in the notification header |
| `environment` | `""` | `KTESTIFY_ENV` | Environment label (e.g. `"staging"`, `"prod"`) |
| `report-url` | `""` | `KTESTIFY_REPORT_URL` | Link to published HTML/Allure report |

### `thresholds` block

| Key | Default | Description |
|---|---|---|
| `good` | `75` | Success rate % ≥ this → `"good"` style (green) |
| `warning` | `50` | Success rate % ≥ this → `"warning"` style (yellow); below → `"attention"` (red) |

### `groups` list

Groups scenarios by Cucumber tag for a per-domain breakdown in the notification card.

```hocon
groups = [
  { tag = "orders",   label = "Order Service",   emoji = "📦" },
  { tag = "payments", label = "Payment Service", emoji = "💳" },
  { tag = "inventory",label = "Inventory",       emoji = "🏭" }
]
```

Scenarios that match no configured tag are placed in an automatic **Untagged** group.

### `channels` list

Each entry configures one outbound channel. Common keys:

| Key | Description |
|---|---|
| `type` | Channel type: `"teams"`, `"slack"`, `"webhook"`, `"log"` |
| `enabled` | Whether this channel fires |
| `on-failure-only` | Only notify when the suite fails (overrides global default) |
| `webhook-url` | Incoming webhook URL (Teams / Slack) |
| `url` | Endpoint URL (webhook type) |
| `method` | HTTP method (webhook type, default `"POST"`) |
| `headers` | HTTP headers map (webhook type) |
| `template` | Template source, see [Templates](#templates) |

---

## Channel Examples

### Microsoft Teams

```hocon
{
  type            = "teams"
  enabled         = true
  webhook-url     = ${?KTESTIFY_TEAMS_WEBHOOK_URL}
  on-failure-only = false
  template        = "builtin"
}
```

The Adaptive Card renders a colour-coded header (green / yellow / red), a fact set with CI/Git metadata, a section per tag group, and action buttons linking to the report and pipeline.

### Slack

```hocon
{
  type            = "slack"
  enabled         = true
  webhook-url     = ${?KTESTIFY_SLACK_WEBHOOK_URL}
  on-failure-only = true   # only notify on failures
  template        = "builtin"
}
```

### Generic HTTP Webhook

```hocon
{
  type    = "webhook"
  enabled = true
  url     = ${?KTESTIFY_WEBHOOK_URL}
  method  = "POST"
  headers {
    Content-Type  = "application/json"
    Authorization = "Bearer "${?KTESTIFY_WEBHOOK_TOKEN}
  }
  template = "builtin"
}
```

### Log (debug / local dev)

```hocon
{
  type    = "log"
  enabled = true
}
```

Zero external dependencies. Always available. Writes suite results and per-group breakdowns to the SLF4J logger at `INFO`.

---

## Templates

The notification payload for each channel is driven by a three-slot template system:

| Slot | Variable | Description |
|---|---|---|
| `suite` | outer card / message body | Rendered once per suite event |
| `group` | per-group section | Rendered once per tag group, injected via `{{groupSections}}` |
| `footer` | action buttons / links | Rendered once, injected via `{{footer}}` |

### Resolution order (highest → lowest priority)

1. **Inline HOCON `"""..."""`** string in `template.suite` / `template.group` / `template.footer`
2. **Classpath resource**, `template = "classpath:my-card.json"`
3. **Filesystem path**, `template = "/opt/config/teams-card.json"`
4. **Bundled default**, `template = "builtin"` (or omitted)

### Inline template example

```hocon
{
  type        = "teams"
  enabled     = true
  webhook-url = ${?KTESTIFY_TEAMS_WEBHOOK_URL}
  template {
    suite = """
      {
        "type": "AdaptiveCard",
        "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
        "version": "1.4",
        "body": [
          {
            "type": "TextBlock",
            "text": "🧪 **{{suiteName}}**, {{environment}}, {{date}}",
            "weight": "Bolder",
            "size": "Large"
          },
          {
            "type": "FactSet",
            "facts": [
              {"title": "Branch",  "value": "{{gitBranch}}"},
              {"title": "Passed",  "value": "{{passedCount}}/{{totalCount}}"},
              {"title": "Success", "value": "{{successRate}}%"}
            ]
          },
          {{groupSections}}{{footer}}
        ]
      }
    """
    group = """
      {
        "type": "Container",
        "style": "{{style}}",
        "items": [{"type": "TextBlock", "text": "{{emoji}} **{{groupLabel}}**, {{successRate}}%"}]
      }
    """
    footer = """
      {
        "type": "ActionSet",
        "actions": [
          {"type": "Action.OpenUrl", "title": "📊 Report",   "url": "{{reportUrl}}"},
          {"type": "Action.OpenUrl", "title": "🔗 Pipeline", "url": "{{pipelineUrl}}"}
        ]
      }
    """
  }
}
```

### Template variable reference

#### Suite-level variables

| Variable | Example |
|---|---|
| `{{suiteName}}` | `"My Integration Tests"` |
| `{{environment}}` | `"staging"` |
| `{{date}}` | `"2026-06-06"` |
| `{{status}}` | `"PASSED"` / `"FAILED"` |
| `{{overallStyle}}` | `"good"` / `"warning"` / `"attention"` |
| `{{totalCount}}` | `"42"` |
| `{{passedCount}}` | `"38"` |
| `{{failedCount}}` | `"4"` |
| `{{skippedCount}}` | `"0"` |
| `{{successRate}}` | `"90"` |
| `{{reportUrl}}` | `"https://pages.gitlab.io/…"` |
| `{{ciName}}` | `"GitHub Actions"` |
| `{{pipelineUrl}}` | `"https://github.com/…/runs/123"` |
| `{{buildNumber}}` | `"456"` |
| `{{gitBranch}}` | `"main"` |
| `{{gitRevision}}` | `"a1b2c3d"` |
| `{{gitTag}}` | `"v1.4.0"` |
| `{{groupSections}}` | *(rendered group fragments, comma-joined)* |
| `{{footer}}` | *(rendered footer fragment)* |

#### Group-level variables (`template.group` only)

| Variable | Example |
|---|---|
| `{{groupLabel}}` | `"Order Service"` |
| `{{emoji}}` | `"📦"` |
| `{{tag}}` | `"orders"` |
| `{{successRate}}` | `"67"` |
| `{{style}}` | `"warning"` |
| `{{passedCount}}` | `"8"` |
| `{{failedCount}}` | `"4"` |
| `{{totalCount}}` | `"12"` |

---

## Custom Channels (SPI)

Implement `NotificationChannel` and register it via the Java SPI:

```java
// com.example.MyPagerDutyChannel.java
public class MyPagerDutyChannel implements NotificationChannel {
    @Override public String getType() { return "pagerduty"; }

    @Override
    public void sendSuite(SuiteEvent event) {
        // send a PagerDuty alert if event.getStatus() == FAILED
    }
}
```

```
# META-INF/services/io.github.ktestify.notifications.channel.NotificationChannel
com.example.MyPagerDutyChannel
```

Then reference it in your config:

```hocon
channels = [{ type = "pagerduty", enabled = true }]
```

---

## Architecture

```
NotificationHooks (@Before / @After / @AfterAll)
       │
       ├─ @Before  → records scenario start time
       │
       ├─ @After   → ScenarioEvent.from(scenario, durationMs)
       │               └─ ScenarioAggregator.record(event)
       │               └─ NotificationService.dispatch(scenarioEvent)  [no-op by default]
       │
       └─ @AfterAll → CiContextResolver.resolve()
                       ScenarioAggregator.buildSuiteEvent(config, ci, git)
                           └─ tag grouping + success-rate + style computation
                       NotificationService.dispatch(suiteEvent)
                           └─ CompletableFuture.runAsync() per channel
                       NotificationService.shutdown(30s)


NotificationService
  └─ List<NotificationChannel>  ← built by NotificationChannelFactory
       ├─ LogNotificationChannel
       ├─ TeamsNotificationChannel  ──► NotificationTemplateEngine.renderSuite()
       ├─ SlackNotificationChannel  │       └─ NotificationTemplateResolver (4-level lookup)
       └─ WebhookNotificationChannel│           └─ BuiltinTemplates (fallback)
                                    └─► HttpClient.send(POST)
```

### Key design rules

- `sendSuite()` **must never throw**, exceptions are caught, logged at `WARN`, and swallowed
- Notifications are **async**, test execution is never blocked by a slow webhook
- The plugin has **zero overhead when disabled**, no channels are instantiated, no threads started

---

## Development

```bash
# Compile
mvn compile

# Unit tests (no Docker required)
mvn test

# Integration tests (requires Docker)
mvn verify

# Code formatting
mvn spotless:apply
```

---

## License

Apache License 2.0, see [LICENSE](LICENSE).


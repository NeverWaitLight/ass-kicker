# ass-kicker-manager

Executable distribution for the Ass Kicker manager service.

## Layout

| Path | Purpose |
|------|---------|
| `bin/` | Foreground launchers and background start/stop scripts |
| `conf/` | Externalized configuration overrides |
| `lib/` | Spring Boot fat jar |
| `run/` `logs/` | Created by background scripts for pid and output |

## Start

Unix or macOS foreground:

```bash
bin/ass-kicker-manager
```

Windows foreground:

```bat
bin\ass-kicker-manager.cmd
```

Unix or macOS background:

```bash
bin/start.sh
bin/stop.sh
```

Windows background:

```bat
bin\start.bat
bin\stop.bat
```

Use `JAVA_OPTS` for extra JVM flags. The service loads overrides from `conf/application.yml`.

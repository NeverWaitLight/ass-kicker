# ass-kicker-worker

Java 21 Spring Boot 可执行发行目录

## 目录说明

| 路径 | 说明 |
|------|------|
| `bin/` | 前台 `ass-kicker` `ass-kicker.cmd` 后台 `start.sh` `stop.sh` `start.bat` `stop.bat` |
| `conf/` | 外部配置 修改 `application.yml` 覆盖默认连接与密钥等 |
| `lib/` | 可运行 JAR（Spring Boot fat jar 单包内含依赖） |
| `run/` `logs/` | 后台启动时由脚本创建 存放 `ass-kicker.pid` 与标准输出日志 |

## 启动

前台 Unix 或 macOS：

```bash
bin/ass-kicker
```

前台 Windows：

```bat
bin\ass-kicker.cmd
```

后台 Unix 或 macOS：

```bash
bin/start.sh
bin/stop.sh
```

后台 Windows：

```bat
bin\start.bat
bin\stop.bat
```

Unix 前台与后台均可通过环境变量 `JAVA_OPTS` 传入 JVM 参数 Windows 后台若需 JVM 参数可使用环境变量 `JAVA_TOOL_OPTIONS` 或由 `start.bat` 自行扩展

## 许可证

见根目录 `LICENSE` 文件

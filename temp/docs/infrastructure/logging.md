# ロギング機能 — 構造図

AstralRecord のロギング機能は **コンソール出力** と **非同期 CSV ファイル出力（監査ログ）** の 2 系統で構成されています。

---

## クラス構成図

```mermaid
classDiagram
    class Logger {
        <<final>>
        +info(message, args) void$
        +warn(message, args) void$
        +error(message, t, args) void$
        +debug(message, args) void$
        +info(logId, args) void$
        +warn(logId, args) void$
        +error(logId, t, args) void$
        +debug(logId, args) void$
        +log(logId, args) void$
    }

    class LogId {
        <<enum>>
        W_1000
        E_1000
        D_1000
        I_3000
        I_3050
        E_1100
        ...
        +getId() String
        +getNumber() int
    }

    class LogMessageProvider {
        <<final>>
        +format(id, args) String$
        +getMessage(id) String$
    }

    class LogEntry {
        <<interface>>
        +toCsvRow() String
        +logDir() String
        +csvHeader() String
        +fileNamePrefix() String
    }

    class LogEntryMeta {
        <<annotation>>
        +logDir() String
        +csvHeader() String
        +fileNamePrefix() String
    }

    class ConsoleLogEntry {
        <<record>>
        timestamp: String
        level: String
        message: String
        +toCsvRow() String
    }

    class AuditLogger~E~ {
        <<final>>
        -logDir: String
        -csvHeader: String
        -fileNamePrefix: String
        -logQueue: ConcurrentLinkedQueue~E~
        -scheduler: ScheduledExecutorService
        +init() void
        +offer(entry) void
        +flush() void
        +shutdown() void
        +initDefault()$ void
        +offerDefault(entry)$ void
        +shutdownDefault()$ void
    }

    class AuditLoggerRegistry {
        <<final>>
        -registry: Map~Class, AuditLogger~$
        +init(basePackage) void$
        +getLogger(clazz) AuditLogger$
        +shutdownAll() void$
    }

    Logger --> LogId : 使用
    Logger --> LogMessageProvider : 使用
    Logger --> AuditLogger : ConsoleLogEntry を offer
    LogMessageProvider --> LogId : ID解決

    AuditLogger --> LogEntry : 型パラメータ制約
    AuditLoggerRegistry --> AuditLogger : 生成・管理
    AuditLoggerRegistry --> LogEntry : スキャン対象
    AuditLoggerRegistry --> LogEntryMeta : アノテーション読取

    ConsoleLogEntry ..|> LogEntry : implements
```

---

## ログ出力フロー（コンソール）

```mermaid
sequenceDiagram
    participant Caller as 任意のクラス
    participant LOG as Logger
    participant LMP as LogMessageProvider
    participant LID as LogId
    participant Console as Bukkit Logger (コンソール)
    participant AL as AuditLogger~ConsoleLogEntry~

    Caller->>LOG: Logger.log(LogId.I_3050)
    LOG->>LID: getId() → "I_3050"
    LOG->>LMP: format("I_3050") → メッセージ文字列
    LOG->>Console: plugin.getLogger().log(Level.INFO, "[INFO] ...")
    LOG->>AL: offer(new ConsoleLogEntry(...))
    AL->>AL: logQueue に追加（非同期）
    Note over AL: スケジューラが定期的にフラッシュ → CSV ファイルに書き出し
```

---

## AuditLogger の非同期書き出しフロー

```mermaid
sequenceDiagram
    participant Caller as offer() 呼び出し元
    participant AL as AuditLogger
    participant Queue as ConcurrentLinkedQueue
    participant Scheduler as ScheduledExecutorService
    participant File as logs/{prefix}-yyyy-MM-dd.csv

    Caller->>AL: offer(entry)
    AL->>Queue: add(entry)

    loop 定期フラッシュ（スケジューラ）
        Scheduler->>AL: flush()
        AL->>Queue: poll() × n件
        AL->>File: BufferedWriter で CSV 行を追記
    end
```

---

## AuditLoggerRegistry の自動スキャンフロー

```mermaid
sequenceDiagram
    participant AR as AstralRecord
    participant ALR as AuditLoggerRegistry
    participant Reflections as Reflections ライブラリ
    participant AL as AuditLogger

    AR->>ALR: init("io.github.maaasu.astralRecord")
    ALR->>Reflections: LogEntry実装クラスをスキャン
    Reflections-->>ALR: Set~Class~LogEntry~~

    loop 各 LogEntry 実装クラス
        ALR->>ALR: @LogEntryMeta アノテーション確認
        alt アノテーションあり
            ALR->>AL: new AuditLogger(logDir, header, prefix)
        else デフォルトコンストラクタあり
            ALR->>AL: new AuditLogger(sampleInstance)
        else どちらもない
            ALR->>ALR: スキップ
        end
        ALR->>AL: init()
        ALR->>ALR: registry.put(clazz, logger)
    end

    ALR-->>AR: 初期化完了
```

---

## LogId 命名規則

| プレフィックス | レベル    | 自動ルーティング先 (`Logger.log()`) |
|:-----------|:--------|:-------------------------------|
| `I_`       | INFO    | `Logger.info()`                |
| `W_`       | WARNING | `Logger.warn()`                |
| `E_`       | ERROR   | `Logger.error()`               |
| `D_`       | DEBUG   | `Logger.debug()` ※デバッグモード時のみ出力 |

> 番号帯の割り当て例: `1000番台` = infrastructure/util、`3000番台` = core/event


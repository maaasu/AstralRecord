# イベントシステム — 構造図

PaperAPI のイベントリスナーを統一的に管理するためのフレームワーク層です。

---

## クラス構成図

```mermaid
classDiagram
    class EventHandler {
        <<interface>>
        +initialize() void
        +cleanup() void
        +getHandlerName() String
        +isEnabled() boolean
    }

    class AbstractEventHandler {
        <<abstract>>
        #handlerName: String
        +initialize() void
        +cleanup() void
        +getHandlerName() String
    }

    class EventManager {
        -plugin: AstralRecord
        -handlers: List~EventHandler~
        +registerAllHandlers() void
        +registerHandler(handler, pm) void
        +unregisterAllHandlers() void
        +getHandlerCount() int
    }

    class ConcreteEventHandler {
        <<将来実装 / feature層>>
        例: PlayerJoinEventHandler
        例: EntityDamageEventHandler
    }

    AbstractEventHandler ..|> EventHandler : implements
    ConcreteEventHandler --|> AbstractEventHandler : extends
    EventManager --> EventHandler : 管理
    EventManager --> ConcreteEventHandler : 登録
```

---

## イベントハンドラの登録フロー

```mermaid
sequenceDiagram
    participant AR as AstralRecord
    participant EM as EventManager
    participant EH as ConcreteEventHandler
    participant PM as PluginManager (PaperAPI)

    AR->>EM: new EventManager(plugin)
    AR->>EM: registerAllHandlers()
    EM->>EM: Logger.log(I_3050) ← 登録開始ログ

    loop 各ハンドラ
        EM->>EM: new ConcreteEventHandler()
        EM->>EM: registerHandler(handler, pm)

        EM->>EH: isEnabled()
        alt 無効
            EH-->>EM: false
            EM->>EM: Logger.log(W_3000, handlerName) ← スキップログ
        else 有効
            EH-->>EM: true
            EM->>EH: initialize()
            EM->>PM: registerEvents(handler, plugin)
            EM->>EM: handlers.add(handler)
            EM->>EM: Logger.log(D_3000, handlerName)
        end
    end

    EM->>EM: Logger.log(I_3051, handlers.size())
    EM-->>AR: 登録完了
```

---

## イベントハンドラの解除フロー（プラグイン停止時）

```mermaid
sequenceDiagram
    participant AR as AstralRecord
    participant EM as EventManager
    participant EH as EventHandler
    participant PM as PluginManager

    AR->>EM: unregisterAllHandlers()
    EM->>EM: Logger.log(I_3052) ← 解除開始ログ

    loop 登録済みの全ハンドラ
        EM->>EH: cleanup()
        alt 例外発生
            EH-->>EM: Exception
            EM->>EM: Logger.log(E_3001, e, handlerName)
        end
    end

    EM->>EM: handlers.clear()
    EM->>EM: Logger.log(I_3053) ← 解除完了ログ
    EM-->>AR: 完了
```

---

## 新しいイベントハンドラの実装手順

新しいゲームイベントを追加する場合の実装パターンです。

```mermaid
graph TD
    A["1. feature/ 配下に新クラスを作成\n例: PlayerJoinEventHandler.java"] --> B
    B["2. AbstractEventHandler を継承\nimplements Listener を追加"] --> C
    C["3. @EventHandler アノテーションで\nイベントメソッドを定義"] --> D
    D["4. EventManager#registerAllHandlers() に\nregisterHandler(...) を追記"]
```

**実装例（概要）:**

```java
// feature/player/PlayerJoinEventHandler.java
public class PlayerJoinEventHandler extends AbstractEventHandler implements Listener {

    @Override
    public boolean isEnabled() { return true; }

    @org.bukkit.event.EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // ゲームロジックを記述
    }
}

// EventManager#registerAllHandlers() に追記
registerHandler(new PlayerJoinEventHandler(), pm);
```

---

## LogId 対応表（イベントシステム）

| LogId    | レベル  | 出力メッセージ                        |
|:---------|:------|:----------------------------------|
| `I_3000` | INFO  | イベントハンドラーが初期化されました: {name}     |
| `I_3001` | INFO  | イベントハンドラーのクリーンアップを実行しました: {name} |
| `I_3050` | INFO  | イベントマネージャーを初期化しています            |
| `I_3051` | INFO  | イベントハンドラーを登録しました。登録数: {count}  |
| `I_3052` | INFO  | 全イベントハンドラーの登録解除を開始します          |
| `I_3053` | INFO  | 全イベントハンドラーの登録解除が完了しました         |
| `W_3000` | WARN  | イベントハンドラーが無効のためスキップします: {name}  |
| `D_3000` | DEBUG | イベントハンドラーを登録しました: {name}        |
| `E_3000` | ERROR | イベントハンドラーの登録に失敗しました: {name}     |
| `E_3001` | ERROR | イベントハンドラーのクリーンアップに失敗しました: {name} |


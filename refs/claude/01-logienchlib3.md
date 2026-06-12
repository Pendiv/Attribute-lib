# LogienchLib-3 精読ノート(attributelib 開発者向け)

対象: `refs/LogienchLib-3/LogienchLib-3/`(Kotlin 2.1.21, マルチモジュール api/core/bukkit/velocity, Paper 1.20.6 / Java 21)
位置づけ: LogienchLib v2(Java)の作り直し。**未完成**(メニュー/アイテム周りは骨だけ、LoreBuilder は空 interface)。
毛色は違う(config/メニュー/タイマーの汎用 util)が、**プロジェクト規律(テスト・バージョンカタログ)と
タイマー設計に持ち帰る価値**がある。それ以外はほぼ却下。

---

## 1. 採用(attributelib にそのまま持ち込む)

### a) テストインフラ(このリポジトリで一番価値がある部分)

- スタック: **MockBukkit + MockK + JUnit5**(`gradle/libs.versions.toml` L21-28。
  「MockBukkit はバージョンを下手に上げると JUnit とズレて壊れる」という実戦コメント付き)。
- `BukkitTestBase.kt`: `@BeforeEach` で `MockBukkit.mock()` + プラグインロード、`@AfterEach` で
  `unmock()` + `clearAllMocks()`。さらに**ベースクラス自身に「テストインフラが動くこと」のテスト**を
  置いている(L48-53)— 環境が壊れた時に最初に赤くなる canary。
- テスト名はバッククォートの日本語文(`` `タスクが正常に実行完了したときにプールから削除されること` ``)。
  Java では使えないが、`@DisplayName` で同じ規律を再現する。
- `BukkitTimerTest.kt` の3本は「登録→tick進行→完了確認」「手動キャンセル」「**退出時掃除**」を
  押さえており、**ライフサイクル系 API のテストの雛形**としてそのまま使える。
  `server.scheduler.performTicks(10)` で時間を進める書き方が肝。
- attributelib への適用: **層1(属性エンジン)はイベントに触れない純粋部分なので MockBukkit すら
  ほぼ不要、素の JUnit で回る**。時限モディファイア・装備同期(層3)で MockBukkit を使う。
  ※ Paper 26.1.2 対応の MockBukkit(現 org.mockbukkit)が存在するかは導入時に要確認。無ければ
  層1だけ素 JUnit でテストし、層2以降は runServer での手動確認+デバッグコマンドに割り切る。

### b) Gradle バージョンカタログ(`gradle/libs.versions.toml`)

依存とプラグインのバージョンを1ファイルに集約し、バージョン根拠をコメントで残す習慣
(「junit-platform 1.11.0 は junit5 5.11.0 と対応」等)。attributelib は依存が少ないので
必須ではないが、**「バージョン選定理由をコメントで残す」習慣だけは輸入**する。

### c) プレイヤー退出時のタスク掃除の「配線」(v2 の最大の罠の修正)

v2 は javadoc で「退出で自動停止」と謳いながら未配線だった(EnhancedMobs ノート 01 §4)。
v3 は `BukkitTimer.kt` L50-53 で `PlayerQuitEvent(HIGHEST)` → `pool.cancel()` を実装し、
**テストで証明している**。「ライフサイクル契約はテストで担保する」の実例。
attributelib の per-entity モディファイアキャッシュも同じ形:
**EntityRemoveFromWorldEvent / PlayerQuitEvent での掃除を実装と同時にテスト**する。

### d) ComponentUtil(`api/text/ComponentUtil.kt` — v2 からの正しい退化)

v2 は自前 § パーサ(数百行)だったが、v3 は **NO_ITALIC ベース + 標準シリアライザ
(LegacyComponentSerializer / MiniMessage / PlainText)に委譲する 85 行**に縮んだ。
- `NO_ITALIC = Component.empty().style(ITALIC=FALSE)` を `empty()/builder()` の土台にする
  (アイテム名/lore のデフォルト斜体打ち消し)— **層4 lore コンポーザーの必須定石**。
- 層4 はこのファイルの形(小さな static util、シリアライザは標準品)をそのまま Java に写す。

---

## 2. 概念だけ借りる(実装は作り直す)

### a) LTask / LTaskPool(`api/timer/Timer.kt`, `core/timer/LTaskPoolImpl.kt`)

- 構造: `LTask`(isFinished/isCancelled/cancel のハンドル)+ `LTaskPool`(完了タスクを
  自動排除する束)+ per-player プール(退出で一括 cancel)。
- 実行側(`BukkitTimer.kt` L67-101): タスク本体を `try { run() } finally { done=true; pool?.remove(task) }`
  で包む — **「完了したら自分をプールから外す」自己後始末**。v2 の AtomicReference 方式より素直。
- attributelib での対応物: **時限モディファイアの期限管理**。ただし企画書 §8-5 の通り
  「永続する時限効果」は **ワールド時間基準**で持つ(tick スケジューラは再起動で消えるため)。
  スケジューラ式プールは「保存しない一時効果」専用。この**二本立ての区別**を層1の API に明示する
  (ShardLib の `isPersistentToMob = persistent && duration<=0` と同じ割り切り)。
- 注意: 無限タイマーは `done` が立たないのでプールが保持し続ける(設計通りだが、attributelib では
  「期限なしモディファイア」はそもそもタイマーに乗せない)。

### b) TimerOption(`api/timer/TimerOption.kt`)

「`for (i=start; test(i); i+=step)` を tick に展開する」一般化。v2 の `Timer.startTimer` と同型で、
**残り時間付きバフの減衰表示**などに使える形。ただし Tick と Duration の二重表現を全コンストラクタで
引き回していて(計7本)冗長 — Paper 専用の attributelib は **tick のみ**でよい。

---

## 3. 却下(attributelib には持ち込まない)

| 対象 | 理由 |
|---|---|
| **InstanceHolder 状態機械**(`api/InstanceHolder.kt`) | `@Deprecated(level=ERROR, "内部API")` で隠した setter + プラグイン側から**リフレクションで private フィールド state を書き換える**(`LogienchLibPlugin.kt` L71-76)。friend アクセスの偽装に過ぎず、単一プラグインなら onEnable で手配線すれば消える複雑さ。`ItemBuilder.of()` → InstanceHolder のサービスロケータ依存も同罪 |
| **Avaje Inject(DI フレームワーク)** | シングルトン5個の結線に DI コンテナは過剰(ShardLib の Guice と同じ病気)。attributelib はコンストラクタ注入の手書きで足りる |
| **LConfig / BoostedYAML**(`api/config/LConfig.kt` 291行) | Bukkit `FileConfiguration` とほぼ同じ API 面を BoostedYAML の上に再発明している。固有の利点はコメント保持アップデート(`loadWithUpdate`)だけで、外部依存1個と維持コストに見合わない。attributelib は Paper 標準 config + 必要なら型付きアクセサを自作 |
| **api/core/bukkit/velocity 4モジュール分割** | Velocity 対応は attributelib に無関係。Timer API が Duration/Tick 両対応で肥大化したのもこの分割が原因。単一モジュールで開始し、利用プラグインが増えて API jar が欲しくなったら ShardLib の apiJar タスク方式(api パッケージだけ別 jar)で済む |
| **メニュー/ItemBuilder 系** | attributelib のスコープ外。LoreBuilder は**空 interface**(未完成の見本) |

---

## 4. 反面教師(踏まないこと)

1. **本番 onEnable にテストコード混入**(`LogienchLibPlugin.kt` L54-68): object Listener が
   **全プレイヤーの PlayerInteractEvent でテストメニューを開く**。ShardCore のチャットデバッグ分岐と
   同じ病気。attributelib のデバッグ口は最初から「コマンド + 権限」で作る。
2. **get-then-put の競合**(`BukkitTimer.kt` PlayerLTaskPoolImpl.get L41-47): ConcurrentHashMap を
   使いながら取得と登録が非アトミック。`computeIfAbsent` を使う。
3. **MockBukkit の都合で本体クラスを open に**(L18-19 のコメント「とっても嫌だけど open に」):
   Java では無関係だが、「テスト都合が本体設計を侵食したら設計を疑う」教訓として。
4. **データクラスの二重表現**(TimerOption の tick/Duration 並走): 対応プラットフォームを
   増やす準備を「今使わないのに」入れるとコンストラクタが7本になる。YAGNI。

---

## 5. 総評

v2 → v3 で作者は明確に成長している(テスト導入・退出掃除の配線・自前パーサ廃止)。
**吸収すべきは「規律」**(テストでライフサイクル契約を担保する、バージョン根拠を書き残す、
自己後始末イディオム)であって、**「構造」ではない**(DI・状態機械・多層モジュールは全部
attributelib の規模に対して過剰)。

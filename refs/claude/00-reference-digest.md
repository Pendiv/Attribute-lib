# 参照プラグイン総覧 — attributelib が借りるもの・捨てるもの

refs/ 配下の4つの参照ソースを、attributelib の4層(企画書 `refs/attributelibの企画書.txt`)に
対応付けて蒸留したダイジェスト。各リポジトリの完全な精読ノートは
`C:\MODs\EnhancedMobs\refs\claude\01-logienchlib.md / 02-shardlib.md / 03-shardcore.md` にある
(LogienchLib-3 のみ本フォルダの `01-logienchlib3.md` が原本)。

| 参照 | 正体 | attributelib にとっての価値 |
|---|---|---|
| `ShardLib-master` | カスタム属性・アイテム基盤(Java, Guice+SQL) | **層1の直接の手本**(設計と実バグの両方) |
| `ShardCore-master` | ShardLib 上のコンテンツ層(Java+Kotlin, 未完成) | **層2・層4の手本**(ダメージ一元化・StructuredLore) |
| `LogienchLib-master` | v2 汎用 util(Java) | 型付きキー・自己後始末タイマー・例外境界 |
| `LogienchLib-3` | v3 作り直し(Kotlin, 未完成) | テスト規律・退出掃除の配線(構造は借りない) |

---

## 層1: 属性エンジン

### 採用する設計(出典: ShardLib §4a の最小骨格)

1. **属性定義** = id + defaultValue(+必要なら派生式)。enum/定数カタログで型安全に。
2. **Modifier** = `sourceId / 対象属性 / 演算(ADD→MULTIPLY→SET, 優先度順) / 値 / 期限`。
3. **保持** = エンティティごとに base / modifiers / 計算結果キャッシュ / dirty フラグ。
4. **計算** = `(base + ΣADD) × ΠMULTIPLY`、SET は最後勝ち。
   **dirty + 取得時遅延再計算**(ShardLib の「手動 recalculateStats 必須」は呼び忘れバグの温床
   — Javadoc に太字注意書きが要る時点で設計負け)。
5. **バニラブリッジ** = max_health 等バニラが処理する属性だけ、再計算の最後に
   Bukkit AttributeModifier(NamespacedKey 固定で必ず上書き)へ一括反映。1関数に集約。

### 周辺の採用部品

- **型付き PDC キー**(LogienchLib v2 `ContainerKey` = PersistentDataType+NamespacedKey の record)。
  PDC アクセスの3悪(キー打ち間違い・型取り違え・get/set の型不一致)を定数1個に畳む。
  キーカタログは1ファイルに集約(grep 一発で全用途が見える)。
- **Ticket パターン**(ShardLib): addModifier の戻り値 = 「この補正を消すクロージャ」。
  時限バフの自動失効はこれ + スケジューラ。
- **時限の二本立て**(ShardLib `isPersistentToMob = persistent && duration<=0` + 企画書 §8-5):
  永続化するのは無期限モディファイアだけ。時限は (a) 保存しない一時効果(tick スケジューラ)か
  (b) ワールド時間基準の期限値を PDC に書く、のどちらかに必ず分類する。
  `Bukkit.getCurrentTick()` は再起動でリセットされるので**永続期限に使わない**。
- **関数の永続化は「キーだけ保存して registry から引き直す」**(ShardLib ProviderCalculation)。
  動的補正(装備依存の計算式など)を保存したくなった時の正解。
- **StackingRule は REPLACE(=remove+add)1種で開始**(ShardLib の5種は未使用機能の見本)。
  sourceId 単位の総入れ替えが装備同期(層3)と相性が良い。
- **登録ウィンドウ→bake→ロック**(ShardLib): 他プラグインが属性を register できるのは
  自分の onEnable 中だけ、ServerLoadEvent で凍結。ただし ShardLib の dryRun 二段リロードは
  リロード機能を作るまで不要。
- **掃除の配線をテストで担保**(LogienchLib-3): per-entity キャッシュは
  EntityRemoveFromWorldEvent / PlayerQuitEvent で掃除し、その動作をテストに書く。
  v2 は「javadoc では自動掃除、実装は未配線」でリークしていた。

### 踏まないバグ(ShardLib 実バグより、層1に直結する分)

- `addOperationModifier` が **sourceId を属性レジストリに照合**(正しくは targetAttributeId)
  → "trait:xxx" 形式の sourceId が全部例外。**引数の取り違えは単体テストで殺す**。
- 非同期スレッドから HashMap 書き込み + recalculate(Join 時 DB ロード)→ データ競合。
  attributelib は**メインスレッド専用と明記**して同期化を一切しない(中途半端が最悪)。
- `OperationList.hasSet()` が常に空(セグメント index の off-by-one)— 「偶然正しく動く」死にコード。

## 層2: ダメージパイプライン

(出典: ShardCore §2 + EnhancedMobs ノート 03 §4-1 — 全部「反面教師込み」)

- **採る**: 単一リスナー固定優先度(LOWEST で計算、間の優先度帯は他プラグインの修飾用に空ける)。
  攻撃/防御の対応は **表駆動**(`Map<攻撃属性, 防御属性>` — CalculateDamage の DAMAGE_DEFENSE_STATS 型)。
  修飾は **DamageContext を1個組んで最後に setDamage 1回**(トレイトごとの重ね掛け禁止)。
- **採る**: 能動 API(`DamageLib.magic(...)`)が撃つダメージを自前リスナーが**再修飾しないための
  マーキング**。ShardCore は素の `damage(20)` がリスナーに拾われて 20 が黙って消える構造バグを
  自分で踏んでいる(`ActiveTestSkill`)。企画書の能動 API はカスタム DamageType を DamageSource に
  乗せるので、**タイプ判定そのものがマーキングを兼ねる** — この一致を崩さないこと。
- **捨てる**: データHP/見た目HP分離(全置換型)。NATURAL_REGENERATION 無効化のような全ワールド介入が
  必要になり、他プラグインとの共存性が最低になる。attributelib は**バニラHPの上で比例再スケール**
  (企画書 §6 の `setDamage(B × F'/F)`)。
- **教訓**: CalculateDamage に `+ nextDouble(0,100)` のテスト残骸が本番に残っていた。
  **計算式に直書きの定数・乱数項を置かない**(必ず名前付き定数 or config)。

## 層3: 装備同期

- **採る**: 装備由来モディファイアは **PDC に書かずメモリ+再計算**(企画書 §8-4 と ShardCore の
  運用が一致)。装備変更時に `removeModifiers("item:mainhand")` → 再生成して add(REPLACE 運用)。
- **採る**: ShardCore PlayerEquipmentGemManager の **事前展開キャッシュ**
  (装備走査は変更時1回、ホットパス(ダメージイベント)は Map 2回引くだけ)。
  ただし ShardCore は装備変更検知が Join 時のみで未完成 — **キャッシュ無効化のタイミング設計が本体**。
- **採る(注意付き)**: バニラ属性分は `attribute_modifiers` コンポーネントに書くだけ(同期はバニラ)。
  EnhancedMobs DESIGN.md §14 の「コンポーネント時代の base-wipe(属性を書くと基礎ステが消える)」
  は層3実装時の最初の実機検証項目。

## 層4: 表示

- **採る**: **StructuredLore**(ShardCore §3-4): `EnumMap<Section, List<Component>>` →
  join(区切り線) → **セクション開始 index を PDC に保存** → 部分更新は逆構築して置換。
  lore を「文字列の羅列」から「構造を持つドキュメント」へ。企画書の「lore 書き込みはコンポーザー
  1箇所」はこれで実装する。
- **採る**: NO_ITALIC 土台 + 標準シリアライザ(LogienchLib-3 ComponentUtil 85行版。
  v2 の自前パーサは読む価値のみ)。
- **捨てる**: ShardCore の「イベント毎にインベントリ全走査で動的 lore 更新」(重い)。
  更新タイミングは絞る(インベントリを開いた時など)。

## 横断: プロジェクト規律

- **テスト**: MockBukkit + ベースクラス + インフラ canary テスト(LogienchLib-3)。
  層1は純粋ロジックなので素 JUnit で密にテストできる — **計算エンジンと Stacking/期限はテスト必須**。
- **Result 型レジストリ検証**(ShardCore ItemRegistry): 登録結果を enum state + メッセージで返し
  起動ログに表組み出力。属性登録 API の戻り値設計にそのまま使う。
- **フェールファスト起動**(ShardCore Startup): 起動エラーは隠さない。ただしフェーズ状態機械は
  attributelib の規模では不要(onLoad/onEnable/ServerLoad の3点で足りる)。
- **例外境界**(LogienchLib v2 MenuEventException): 利用側プラグインのコールバック
  (動的補正の計算関数など)は try-catch + 出所つきログで包み、1個の例外で
  パイプライン全体を落とさない。
- **デバッグ口は最初からコマンド+権限**(ShardCore のチャット分岐・LogienchLib-3 の
  Interact テストリスナーという2つの前科)。
- **継承より合成**: ArrayList 公開継承(v2 RandomChoice)、Bukkit イベントの私的継承(v2)は禁止。
- **god class 禁止**: SuperItemStack 2286行の教訓 — 必要メソッドだけ切り出す。

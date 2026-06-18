# attributelib

カスタム属性・ダメージ計算・装備同期・表示を提供する Paper プラグイン基盤ライブラリ。
MOD 環境(Forge attribute / L2DamageTracker 相当)の概念を Paper に持ち込む。

- **対象環境**: Paper 26.1.2 / Java 25(専用)
- **ライセンス**: [MIT](LICENSE)

## 構成(4層)

| 層 | 内容 |
|---|---|
| 1. 属性エンジン | カスタム属性の登録・計算・モディファイア管理(恒久/時限)。エンティティ PDC に保持 |
| 2. ダメージパイプライン | 元素(物理/魔法/炎/雷/自作)・会心・防具貫通・全体倍率を、順序保証された単一リスナーで処理 |
| 3. 装備同期 | アイテムに書いた属性を装備中だけエンティティへ反映 |
| 4. 表示 | バニラのツールチップ文法に沿ったlore 自動生成(コンポーザー1箇所集約) |

### 設計の要点

- **毎 tick 処理ゼロ**。時限モディファイアはワールド時間基準の遅延判定(読む瞬間に失効)なので、スケジューラ不要で再起動・チャンクアンロードを跨いでも正確
- **書き込み透過 PDC**。永続データは変更のたびに即保存。アンロード時の保存処理が無く、保存漏れによるデータ消失が構造的に起きない
- **ホットパス最適化**。attributelib のデータを持たないエンティティのダメージ/回復イベントは状態を生成せず素通し
- **メインスレッド専用**(非同期から触ると即例外)

## 導入

1. `attributelib-x.x.jar` を `plugins/` に入れる
2. 初回起動時にカスタムダメージタイプのデータパックが `world/datapacks/attributelib/` へ自動配備される。**初回のみ再起動が1回必要**(起動ログに案内が出る)
3. 利用側プラグインの `plugin.yml` に依存を書く:

```yaml
depend: [attributelib]
```

## 使い方

### 層1: カスタム属性

```java
// onEnable で登録(返り値を定数に保持)
AttributeType DODGE = Attributes.register(this, "dodge_chance", 0.0, 0.0, 1.0,
        Component.text("回避率"), true);   // 表示名と%表示は省略可

double v = Attributes.get(mob, DODGE);                  // 最終値(再計算・失効処理は自動)
Attributes.setBase(mob, DODGE, 0.05);                   // 基礎値(PDC 保存)
ModifierHandle h = Attributes.add(mob, DODGE,           // 恒久モディファイア
        "myplugin:trait/swift", Operation.ADD, 0.1);
Attributes.add(mob, DODGE, "myplugin:potion",           // 200tick の時限(自動失効)
        Operation.MULTIPLY, 1.5, 200);
Attributes.addTransient(mob, DODGE, "myplugin:aura",    // 保存しない一時効果
        Operation.ADD, 0.2);
Attributes.removeAll(mob, "myplugin:trait/swift");      // sourceId 単位で一括除去
h.remove();                                             // チケットで個別解除
```

計算式: `final = clamp((base + ΣADD) × ΠMULTIPLY)`、SET があれば最後の SET が勝つ。

### バニラ属性(ブリッジ)

全バニラ属性(36種)が `Vanilla.*` 定数としてブリッジ登録されており、カスタム属性と
完全に同じ API で操作できる。バニラ単体では不可能な「時限つきバニラ属性」も1行:

```java
// 30秒だけ最大HP+10(再起動・アンロードを跨いでも正確に失効)
Attributes.add(mob, Vanilla.MAX_HEALTH, "myplugin:trait/tank", Operation.ADD, 10, 600);
Attributes.removeAll(mob, "myplugin:trait/tank");

// 採掘速度: プレイヤーに直接、またはツルハシ(装備中のみ有効)に
Attributes.add(player, Vanilla.BLOCK_BREAK_SPEED, "myplugin:buff", Operation.MULTIPLY, 1.5, 200);
ItemAttributes.add(pickaxe, Vanilla.MINING_EFFICIENCY, Operation.ADD, 10, EquipmentSlotGroup.MAINHAND);
```

仕組み: 管理は attributelib 側が持ち、合成結果だけを transient なバニラモディファイアとして
書き込む(NBT に残らないのでゴースト強化なし)。時限の失効は「期限ちょうどに1回だけ」の
タスクで反映する(毎 tick 監視はしない)。`Attributes.get` はバニラの最終値
(他プラグインのモディファイア込み)を返す。

生のバニラ API に近い冪等ヘルパーが必要なら `VanillaAttributes.set / setTransient / remove` もある。

### 適用条件

モディファイアに条件を付けると、成立している間だけ効く。カスタム属性・バニラブリッジ・
アイテムのどれでも同じに使える:

```java
// 夜間のみ攻撃力+3
Attributes.add(mob, Vanilla.ATTACK_DAMAGE, "myplugin:trait/nocturnal",
        Operation.ADD, 3, Conditions.NIGHT);

// 水中のみ採掘速度2倍のツルハシ(lore に「(水中)」が自動表示)
ItemAttributes.add(pickaxe, Vanilla.SUBMERGED_MINING_SPEED,
        Operation.MULTIPLY, 2.0, EquipmentSlotGroup.MAINHAND, Conditions.IN_WATER);

// 独自条件も1行(キーだけが保存され、評価時にレジストリから引き直される)
Condition BOSS_NEARBY = Conditions.register(this, "boss_nearby",
        Component.text("ボス接近中"), entity -> ...);
```

標準条件: `DAY / NIGHT / IN_WATER / UNDERWATER / IN_LAVA / IN_RAIN / BURNING /
IN_OVERWORLD / IN_NETHER / IN_THE_END / SNEAKING / ON_GROUND / FULL_HEALTH`

ポーション効果の保持判定は全エフェクト分が自動登録済み(`attributelib:effect/<名前>`):

```java
// 毒状態の間だけ被ダメ+50%
Attributes.add(mob, StandardAttributes.DAMAGE_TAKEN, "myplugin:trait/frail",
        Operation.ADD, 0.5, Conditions.hasEffect(PotionEffectType.POISON));
```

合成(AND / OR / NOT)も1行 — 合成結果も独立した条件キーを持つので永続化・lore 表示が効く:

```java
Condition NETHER_BURNING = Conditions.allOf(this, "nether_burning",
        Conditions.IN_NETHER, Conditions.BURNING);
Condition NOT_NIGHT = Conditions.not(this, "not_night", Conditions.NIGHT);
Condition WET = Conditions.anyOf(this, "wet", Conditions.IN_WATER, Conditions.IN_RAIN);
```

仕組み: カスタム属性は読み取りの瞬間に評価(コストはイベント時のみ)。バニラブリッジは
条件の反転を周期タスクが書き戻す — このタスクは「条件付きブリッジを持つエンティティが
存在する間だけ」稼働し、値が変わらなければ書き込みもしない。提供元プラグインが抜けて
未解決になった条件は「不成立」として扱われる(効果が残留する側に倒さない)。

### 層2: ダメージ

標準属性(全エンティティで利用可): `damage_dealt` `damage_taken` `crit_chance` `crit_damage`
`crit_resist` `armor_penetration` `armor_penetration_flat` `heal_multiplier` と、
元素ペア `physical / magic / fire / lightning` の `_damage` / `_resist`。

```java
// 能動ダメージ(被害者側イベントを通るので受動修飾と同一系で処理される)
DamageLib.magic(attacker, victim, 8);      // 魔法(防具素通り、magic_resist が効く)
DamageLib.pierce(attacker, victim, 5);     // 物理貫通
DamageLib.trueDamage(attacker, victim, 3); // 確定(全防御・無敵フレーム素通り)

// 元素の追加は1行 — <id>_damage / <id>_resist / <id>_damage_flat 属性が自動で生える
DamageElement VOID = DamageElements.register(this, "void",
        Component.text("虚与ダメージ"), Component.text("虚耐性"));
DamageLib.deal(VOID, attacker, victim, 8);         // 宣誓ダメージ(防具を尊重・貫通が効く)
DamageLib.dealPiercing(VOID, attacker, victim, 8); // 宣誓 + この一撃だけ防具を完全貫通
DamageElements.mapDamageType(myDamageTypeKey, VOID);    // 自作ダメージタイプの紐付け
```

宣誓ダメージ(`deal`)は既定でバニラ防具を**尊重**し、攻撃側の `armor_penetration` /
`armor_penetration_flat` と上限超過軽減が効く(貫通は防具軽減と超過軽減の両方を削る)。
防具を完全に無視したい一撃だけ `dealPiercing` を使う(臨時・その呼び出し限り)。

バニラの炎(`in_fire`/`lava`/…)は `fire_resist`、雷(`lightning_bolt`)は `lightning_resist` が
そのまま効く(紐付け済み)。

### 層3+4: 装備とツールチップ

```java
ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
ItemAttributes.add(sword, StandardAttributes.CRIT_CHANCE,
        Operation.ADD, 0.3, EquipmentSlotGroup.MAINHAND);
// → 装備中だけ会心率+30%。lore に青文字で「+30% 会心率」が自動表示される
```

- 同期は `EntityEquipmentChangedEvent`(プレイヤー・モブ・全スロット対応)
- lore はバニラの色とルール(青=プラス、赤=マイナス、深緑=SET、灰色ヘッダはクライアント言語で翻訳)
- 既存の lore(説明文)は壊さない。バニラ属性の表示を変えたい場合は
  `ItemAttributes.setVanilla(..., AttributeModifierDisplay.override(...))`

## ステータスサイドバー(/sideboard)

プレイヤーが最大3ジャンルを選んで、選択順に上から表示する内蔵サイドバー(依存なし):

```
/sideboard combat resist user   選択順に表示(タブ補完あり)
/sideboard on / off             表示切り替え(選択は記憶され、再ログインでも復元)
```

標準ジャンル: `combat`(戦闘系) / `resist`(耐性) / `status`(ステータス) / `user`(ユーザー情報)。

ジャンルも行も外部プラグインから拡張できる。標準行は ID を持つので差し替えも可能:

```java
// ステータスに現在座標を追加
Sideboard.addLine("status", Sideboard.line("position",
        Component.text("座標", NamedTextColor.GREEN),
        p -> Component.text(p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ())));

// 標準の難易度行を自前の値に差し替え(例: EnhancedMobs の危険度)
Sideboard.removeLine("user", "difficulty");
Sideboard.addLine("user", Sideboard.line("difficulty",
        Component.text("難易度", NamedTextColor.WHITE), p -> Component.text(myDanger(p))));

// 新ジャンルの追加(クエスト等)
Sideboard.registerGenre(this, "quest", Component.text("クエスト:", NamedTextColor.GOLD), List.of(...));
```

更新は1秒間隔で、表示中のプレイヤーがいる間だけタスクが動く。

## PlaceholderAPI 連携

PlaceholderAPI 導入時は自動で拡張が登録され、スコアボード・TAB・ホログラム等から
属性値を表示できる(未導入なら何もしない):

```
%attributelib_<属性>%        最終値(％表示属性は「25%」形式)   例: %attributelib_crit_chance%
%attributelib_raw_<属性>%    生の数値                           例: %attributelib_raw_max_health%
%attributelib_base_<属性>%   基礎値
%attributelib_cond_<条件>%   条件の成否(true/false)            例: %attributelib_cond_night%
```

属性・条件は `ns:id` 形式か、名前空間省略(attributelib → minecraft の順で解決)。

## デバッグコマンド(`/alib`、OP 権限)

| コマンド | 内容 |
|---|---|
| `/alib list` | 登録済み属性の一覧 |
| `/alib dump` | 視線の先(いなければ自分)の属性状態 |
| `/alib setbase <ns:id> <値>` / `/alib add <ns:id> <演算> <値> [tick]` | 属性操作 |
| `/alib item [<ns:id> <演算> <値> [slot]]` | 手持ちアイテムの属性表示/追加 |
| `/alib itemclear` / `/alib clear` | 除去 |
| `/alib smoke` | 自己診断(コンソール可。全層を実機検証) |

## ビルド

```
gradlew build      # テスト込み
gradlew runServer  # 検証用サーバー起動(Paper 26.1.2)
```

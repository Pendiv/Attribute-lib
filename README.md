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

### 層2: ダメージ

標準属性(全エンティティで利用可): `damage_dealt` `damage_taken` `crit_chance` `crit_damage`
`crit_resist` `armor_penetration` `armor_penetration_flat` `heal_multiplier` と、
元素ペア `physical / magic / fire / lightning` の `_damage` / `_resist`。

```java
// 能動ダメージ(被害者側イベントを通るので受動修飾と同一系で処理される)
DamageLib.magic(attacker, victim, 8);      // 魔法(防具素通り、magic_resist が効く)
DamageLib.pierce(attacker, victim, 5);     // 物理貫通
DamageLib.trueDamage(attacker, victim, 3); // 確定(全防御・無敵フレーム素通り)

// 元素の追加は1行 — <id>_damage / <id>_resist 属性が自動で生える
DamageElement VOID = DamageElements.register(this, "void",
        Component.text("虚与ダメージ"), Component.text("虚耐性"));
DamageLib.deal(VOID, attacker, victim, 8);              // 宣誓ダメージ(データパック不要)
DamageElements.mapDamageType(myDamageTypeKey, VOID);    // 自作ダメージタイプの紐付け
```

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

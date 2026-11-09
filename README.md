# Barrel Roll Handling

## English

**Barrel Roll Handling** adds simulated physics and limits to elytra flight in [Do a Barrel Roll](https://modrinth.com/mod/do-a-barrel-roll), making it calmer and more stable.
You turn toward the side you bank, the nose drops in a steep climb or a stall, and the faster you go, the steadier you fly and the more you level out.

### About Do a Barrel Roll

Do a Barrel Roll by enjarai is a wonderful mod that brought a whole new way to enjoy the elytra, and I offer its author my respect and admiration.
This mod adds calculations, based on the values you set, through Do a Barrel Roll's public API. It contains none of Do a Barrel Roll's code, so please download and install Do a Barrel Roll separately.
It is not affiliated with or endorsed by Do a Barrel Roll or its author. Please report problems with this mod here, not to Do a Barrel Roll.

### Playing on online servers

Even though this is a client-side mod, some servers forbid mods that assist flight. Before using it on a server, make sure the server's rules allow it.

### Features

Except for the smooth thrust, all of these only turn the direction you are looking (the nose) and never change your speed directly.
Changes to thrust stay within Do a Barrel Roll's own thrust and never push harder than it would. Whether thrust is allowed on a server is decided by Do a Barrel Roll, unchanged.
Each one can be switched off or tuned.

- **Bank to turn**: bank to one side and you turn toward it, like a plane.
- **Bank sink**: in a steep bank, less lift holds the nose up, so it drops.
- **Climb drop**: in a steep climb, the nose returns downward, toward the ground.
- **Stall drop**: below a set speed, the nose falls.
- **Speed lift**: the faster you go, the more the nose returns to level.
- **Roll limit**: softly stops the roll at a set angle and turns the rest into a level turn.
- **Smooth thrust and brake**: Do a Barrel Roll's thrust builds up and fades out gradually, and the brake slows you without flying you backward. Needs Do a Barrel Roll's thrust switched on, and allowed by the server.
- **Landing lets go**: walking keys held when you land count as released until you let go of them, so the key you used to steer or brake doesn't walk you off a ledge. Choose "Back only" or "All" (all four, for when Do a Barrel Roll's yaw is on A/D). Works with controllers through Controlify, and without Do a Barrel Roll.
- **Five presets**, from Gentle to Very sharp. Pick one, apply it, then fine-tune.

### Requirements

- Minecraft 26.3, Fabric Loader 0.20.1 or later, Java 25.
- Client only. Nothing is needed on the server.
- **Do a Barrel Roll 3.8 or later (below 4)** for the flight calculations. Without it, only "Landing lets go" works.
- Optional: Mod Menu for the settings screen. With YetAnotherConfigLib it uses YACL's screen, otherwise its own screen with tabs.
- This mod itself does not need Fabric API (Do a Barrel Roll needs Fabric API or Cicada).

### Tips

- When using "Bank to turn", turning off Do a Barrel Roll's own Banking is recommended. Banking also adds turning and nose drop from your bank angle, so with both on, the effect doubles. The settings screen has a tab with Banking and a few other Do a Barrel Roll settings, changed only when you change them there.

### License

MIT.

---

## 日本語

**Barrel Roll Handling** は、[Do a Barrel Roll](https://modrinth.com/mod/do-a-barrel-roll) のエリトラ飛行に仮想的な物理計算と制限を加え、穏やかに安定させる mod。
傾けた方へ曲がり、急な上昇や失速では機首が下がり、速度が乗るほど安定して水平へ向く。

### Do a Barrel Roll について

enjarai 氏の Do a Barrel Roll は、エリトラに新しい楽しみ方を加えた素晴らしい mod であり、同氏に敬意と賛辞を表する。
この mod は、Do a Barrel Roll の公開された API を通して、設定した値に基づく計算を加えるもの。Do a Barrel Roll のコードは含まないので、別途入手して導入する。
Do a Barrel Roll やその作者とは無関係で、公認も受けていない。この mod の不具合は、Do a Barrel Roll 側ではなく、こちらへ報告してほしい。

### オンラインサーバーで使う時は

クライアントの mod であっても、サーバーによっては飛行を補助する mod を禁止している。使う前に、そのサーバーのルールで許可されていることを確かめること。

### できること

滑らかな推力を除き、どれも視点(機首)の向きを回すだけで、速度は直接変えない。
推力への変更は Do a Barrel Roll の推力の範囲内に収まり、Do a Barrel Roll が出す以上には押さない。サーバーでの推力の許可も、Do a Barrel Roll の仕組みにそのまま従う。
それぞれオフにしたり、強さを変えたりできる。

- **バンクで旋回**:横に傾けると、飛行機のようにその向きへ曲がる。
- **バンクで機首下げ**:深く傾けると、機首を支える揚力が減って機首が下がる。
- **上昇で機首下げ**:急な上昇では、機首が下(地面の方向)へ戻る。
- **失速で機首下げ**:決めた速さより遅いと、機首が落ちる。
- **速度で機首上げ**:速いほど、機首が水平へ戻る。
- **ロールの上限**:決めた角度でロールを柔らかく止め、超えた分を水平の旋回に変える。
- **滑らかな推力とブレーキ**:Do a Barrel Roll の推力が徐々に強まり、徐々に弱まる。ブレーキは後ろへ飛ばずに減速する。Do a Barrel Roll の推力をオンにし、サーバーが許可している必要がある。
- **着地で離すキー**:着地の時に押していた歩くキーを、一度離すまで押していない扱いにする。舵やブレーキに使ったキーで、そのまま崖から歩き出さない。「後ろのみ」か「すべて」(前後左右。Do a Barrel Roll のヨーが A/D の時に)を選べる。Controlify 経由のコントローラーにも効き、Do a Barrel Roll が無くても使える。
- **プリセット5つ**:穏やかからとても鋭いまで。選んで適用してから、細かく調整できる。

### 動作環境

- Minecraft 26.3、Fabric Loader 0.20.1 以上、Java 25。
- クライアントのみ。サーバーには何も要らない。
- 飛行の計算には **Do a Barrel Roll 3.8 以上(4 未満)** が必要。無い時は「着地で離すキー」だけが動く。
- 任意:設定画面に Mod Menu。YetAnotherConfigLib があればその画面、無ければタブ付きの独自の画面。
- この mod 自体は Fabric API を必要としない(Do a Barrel Roll は Fabric API か Cicada が必要)。

### ヒント

- 「バンクで旋回」を使う時は、Do a Barrel Roll 側の Banking をオフにするのがおすすめ。Banking も傾きから旋回と機首下げを加えるので、両方オンだと二重に効く。設定画面に、Banking など Do a Barrel Roll のいくつかの設定を並べたタブがある。画面で変えた時だけ書き換える。

### ライセンス

MIT。

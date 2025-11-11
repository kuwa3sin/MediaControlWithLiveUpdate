# MediaCtlWithLiveUpdate

Jetpack Compose と Material 3 Expressive を用いたメディア制御サンプルアプリです。ライブ更新される通知チップと、権限・カスタマイズ設定をタブで切り替えられる設定画面を備えています。

## 主な機能

- メディア再生状態に応じて更新されるライブ通知 UI
- 権限要求パネルとカスタマイズ設定をタブで切り替え可能な設定画面
- Material 3 Expressive テーマに基づくビジュアルデザイン
- シェアドプリファレンスを用いたチップ表示モードの保存

## 開発環境

- Android Studio Iguana 以降を推奨
- Kotlin 1.9 以上
- Gradle 8 以上（プロジェクト同梱の Gradle Wrapper を使用）

## ビルド & 実行

1. リポジトリをクローンし、Android Studio で開きます。
2. `local.properties` に Android SDK のパスが設定されていることを確認します。
3. 「Run > Run 'app'」 でエミュレータまたは実機にデプロイします。

コマンドラインの場合は次のコマンドでデバッグビルドを生成できます。

```bash
./gradlew assembleDebug
```

## テスト

Compose コンポーネントのスナップショットやユニットテストが追加されている場合は、以下で実行できます。

```bash
./gradlew testDebugUnitTest
```

## ライセンス

このプロジェクトのライセンスは未設定です。利用条件を明確にする場合は `LICENSE` ファイルを追加してください。

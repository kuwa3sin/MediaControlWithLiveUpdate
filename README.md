# MediaCtlWithLiveUpdate

Jetpack Compose と Android 16 の Live Update API を活用したメディア制御サンプルアプリです。ライブ更新される通知チップと、権限・カスタマイズ設定をタブで切り替えられる設定画面を備えています。

> この README を含むドキュメントは、ほぼ全文を GitHub Copilot の支援を受けて記述しています。

## 主な機能

- メディア再生状態に応じて更新されるライブ通知 UI
- 権限要求パネルとカスタマイズ設定をタブで切り替え可能な設定画面
- シェアドプリファレンスを用いたチップ表示モードの保存

## 動作要件

- Android 16 (API 35) 以降の実機またはエミュレータ（Live Update API 対応が前提）
- Android Studio Iguana 以降を推奨
- Kotlin 1.9 以上
- Gradle 8 以上（プロジェクト同梱の Gradle Wrapper を使用）

## 開発環境

上記の動作要件を満たした環境での開発を想定しています。

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

このプロジェクトは [MIT License](LICENSE) の下で公開されています。

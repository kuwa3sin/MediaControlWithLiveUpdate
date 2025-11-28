# ライブメディアコントロール / Live Media Control

Jetpack Compose と Android 16 の Live Update API を活用したメディア制御サンプルアプリです。

メディアの状態に合わせて更新されステータスバーに常駐する通知チップと、権限・カスタマイズ設定をタブで切り替えられる設定画面を備えています。

> この README を含むドキュメントは、GitHub Copilot の支援を受けて記述しています。

## 主な機能

- メディア再生状態に応じて更新されるライブ通知 UI
- 権限要求パネルとカスタマイズ設定をタブで切り替え可能な設定画面
- シェアドプリファレンスを用いたチップ表示モードの保存
- 日本語/英語対応

## 動作要件

- Android 16 (API 35) 以降の実機またはエミュレータ（Live Update API 対応が前提）
- Android Studio Iguana 以降を推奨
- Kotlin 1.9 以上
- Gradle 8 以上（プロジェクト同梱の Gradle Wrapper を使用）

## apk
https://github.com/kuwa3sin/MediaControlWithLiveUpdate/releases/tag/apk

Google Pixel 10 Pro (Android 16 QPR2)での動作は確認済み

## ライセンス

このプロジェクトは [MIT License](LICENSE) の下で公開されています。

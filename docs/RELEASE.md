# Release Process

このドキュメントでは、GitHub PRs Visualizer のリリースプロセスについて説明します。

## 概要

このプロジェクトでは、自動化されたリリースワークフローを使用しています。`release/*` ブランチの PR が `main/master` にマージされると、自動的にビルドが実行され、GitHub Release にアーティファクトが添付されます。

## バージョン管理

バージョン番号は `version.properties` ファイルで管理されています。形式は `major.minor.patch` (例: `1.0.0`) です。

### バージョンの更新方法

1. GitHub リポジトリの "Actions" タブを開く
2. "Bump Version" ワークフローを選択
3. バージョンタイプを選択:
   - `patch`: バグ修正やマイナーな変更 (1.0.0 → 1.0.1)
   - `minor`: 新機能の追加 (1.0.0 → 1.1.0)
   - `major`: 破壊的変更 (1.0.0 → 2.0.0)
4. "Run workflow" を実行
5. ワークフローが `release/v{version}` ブランチと PR を自動作成

## リリースワークフロー

### 自動リリース

`release/*` ブランチの PR が `main/master` にマージされると、以下が自動的に実行されます：

1. **ビルド**: 3つのプラットフォーム (Ubuntu, macOS, Windows) で並行してビルド
2. **パッケージ作成**:
   - Ubuntu: `.deb` パッケージ
   - macOS: `.dmg` パッケージ
   - Windows: `.msi` パッケージ
3. **GitHub Release 作成**: `version.properties` のバージョンに基づいて `v{version}` タグで Release を作成
4. **アーティファクトのアップロード**: すべてのパッケージを Release に添付

### ワークフローファイル

- `.github/workflows/release.yml`: リリース自動化ワークフロー
- `.github/workflows/bump-version.yml`: バージョン更新 + リリース PR 作成ワークフロー
- `.github/workflows/build.yml`: PR/プッシュ時のビルド検証ワークフロー

## リリース手順

通常のリリースフロー：

1. "Bump Version" ワークフローを実行する
2. 自動作成された `release/v{version}` PR を確認する
3. PR を `main/master` にマージする
4. マージ後に自動でリリースが作成される

**注意**: 同じバージョンタグが既に存在する場合、リリースはスキップされます。新しいリリースを作成する前にバージョンを更新してください。

## トラブルシューティング

### リリースが作成されない

- `version.properties` のバージョンに対応するタグ (`v{version}`) が既に存在していないか確認
- GitHub Actions のログで詳細なエラーメッセージを確認

### ビルドが失敗する

- ローカルで `./gradlew :composeApp:packageReleaseDistributionForCurrentOS` を実行して問題を特定
- 依存関係やビルド設定に問題がないか確認

# Release Process

このドキュメントでは、GitHub PRs Visualizer のリリースプロセスについて説明します。

## 概要

このプロジェクトでは、自動化されたリリースワークフローを使用しています。master ブランチにマージされると、自動的にビルドが実行され、GitHub Release にアーティファクトが添付されます。

## バージョン管理

バージョン番号は `version.properties` ファイルで管理されています。形式は `major.minor.patch` (例: `1.0.0`) です。

### バージョンの更新方法

バージョンを更新するには、2つの方法があります：

#### 方法1: GitHub Actions を使用 (推奨)

1. GitHub リポジトリの "Actions" タブを開く
2. "Bump Version" ワークフローを選択
3. "Run workflow" をクリック
4. バージョンタイプを選択:
   - `patch`: バグ修正やマイナーな変更 (1.0.0 → 1.0.1)
   - `minor`: 新機能の追加 (1.0.0 → 1.1.0)
   - `major`: 破壊的変更 (1.0.0 → 2.0.0)
5. "Run workflow" を実行

#### 方法2: 手動で更新

1. `version.properties` ファイルを編集
2. `version=` の値を更新
3. 変更をコミットして master にマージ

## リリースワークフロー

### 自動リリース

master ブランチにプッシュすると、以下が自動的に実行されます：

1. **ビルド**: 3つのプラットフォーム (Ubuntu, macOS, Windows) で並行してビルド
2. **パッケージ作成**:
   - Ubuntu: `.deb` パッケージ
   - macOS: `.dmg` パッケージ
   - Windows: `.msi` パッケージ
3. **GitHub Release 作成**: `version.properties` のバージョンに基づいて `v{version}` タグで Release を作成
4. **アーティファクトのアップロード**: すべてのパッケージを Release に添付

### ワークフローファイル

- `.github/workflows/release.yml`: リリース自動化ワークフロー
- `.github/workflows/bump-version.yml`: バージョン更新ワークフロー
- `.github/workflows/build.yml`: PR/プッシュ時のビルド検証ワークフロー

## リリース手順

通常のリリースフロー：

1. 機能開発を PR で行う
2. PR を master にマージ
3. 必要に応じて "Bump Version" ワークフローを実行してバージョンを更新
4. master にプッシュされると自動的にリリースが作成される

**注意**: 同じバージョンタグが既に存在する場合、リリースはスキップされます。新しいリリースを作成する前にバージョンを更新してください。

## トラブルシューティング

### リリースが作成されない

- `version.properties` のバージョンに対応するタグ (`v{version}`) が既に存在していないか確認
- GitHub Actions のログで詳細なエラーメッセージを確認

### ビルドが失敗する

- ローカルで `./gradlew :composeApp:packageReleaseDistributionForCurrentOS` を実行して問題を特定
- 依存関係やビルド設定に問題がないか確認

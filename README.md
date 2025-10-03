# PDF リネーミングツール

## 概要

PDF リネーミングツールは、指定されたディレクトリとそのサブディレクトリ内のすべての PDF ファイルを再帰的に検索し、各 PDF ファイルのタイトルプロパティに基づいてファイル名を自動的に変更する Groovy CLI コマンドです。

## 主な機能

- **再帰的ディレクトリ探索**: サブディレクトリも含めて全てのPDFファイルを処理
- **PDFメタデータ抽出**: PDFファイルのタイトルプロパティを読み取り
- **ファイル名サニタイズ**: Windowsで使用できない文字を自動的に置換
- **除外条件**: 特定のパターンのファイルを処理から除外
- **多言語対応**: UTF-8、Shift-JIS、EUC-JP文字エンコーディングをサポート
- **安全な2段階処理**: まずスクリプトを生成し、確認後に実行

## 使用方法

### 基本的な使用方法

```bash
# 現在のディレクトリを処理
groovy generatePDFRenameIt.groovy

# 特定のディレクトリを処理
groovy generatePDFRenameIt.groovy /path/to/pdf/directory

# 文字エンコーディングを指定
groovy generatePDFRenameIt.groovy /path/to/pdf/directory UTF-8
```

### サポートされている文字エンコーディング

- `UTF-8` (デフォルト)
- `Shift-JIS`
- `EUC-JP`

### 処理の流れ

1. **スクリプト実行**: `generatePDFRenameIt.groovy` を実行
2. **スクリプト生成**: `renamePDFs.groovy` が生成される
3. **内容確認**: 生成されたスクリプトの内容を確認
4. **リネーム実行**: `groovy renamePDFs.groovy` でファイルをリネーム

## 除外条件

以下の条件に該当するPDFファイルはリネーム対象から除外されます：

- タイトルが空文字列
- タイトルが `_` で始まる
- タイトルが `~` で始まる  
- タイトルが `Untitled` で始まる

## ファイル名サニタイズ

Windowsファイルシステムで使用できない以下の文字は `_` に置換されます：

- `\` (バックスラッシュ)
- `/` (スラッシュ)
- `:` (コロン)
- `*` (アスタリスク)
- `?` (クエスチョン)
- `"` (ダブルクォート)
- `<` (小なり)
- `>` (大なり)
- `|` (パイプ)

## 実行例

### 例1: 基本的な使用

```bash
$ groovy generatePDFRenameIt.groovy
=== PDF リネーミングツール開始 ===
対象ディレクトリ: C:\Users\Example\Documents
文字エンコーディング: UTF-8
出力ファイル: C:\Users\Example\Documents\renamePDFs.groovy

処理中: C:\Users\Example\Documents
リネーム予定: document1.pdf -> 重要な資料.pdf
除外: document2.pdf -> タイトル: '_private_document'
処理中: C:\Users\Example\Documents\subfolder
リネーム予定: report.pdf -> 月次レポート2024.pdf

=== 処理完了 ===
実行時間: 2.3 秒
生成されたスクリプト: C:\Users\Example\Documents\renamePDFs.groovy
スクリプトを実行してファイルをリネームしてください。
```

### 例2: 生成されたスクリプトの実行

```bash
$ groovy renamePDFs.groovy
リネーム: C:/Users/Example/Documents/document1.pdf -> C:/Users/Example/Documents/重要な資料.pdf
リネーム: C:/Users/Example/Documents/subfolder/report.pdf -> C:/Users/Example/Documents/subfolder/月次レポート2024.pdf
```

## エラーハンドリング

- **アクセス権限エラー**: 権限のないディレクトリはスキップして処理継続
- **PDF読み込みエラー**: 破損したPDFファイルはスキップして処理継続
- **文字エンコーディングエラー**: 無効なエンコーディングはUTF-8にフォールバック

## 注意事項

- **バックアップ推奨**: 重要なファイルは事前にバックアップを取ってください
- **スクリプト確認**: 生成された `renamePDFs.groovy` の内容を実行前に確認してください
- **シンボリックリンク**: シンボリックリンクは処理対象から除外されます
- **大量ファイル**: 大量のファイルがある場合は処理に時間がかかる場合があります

## 依存関係

- **Groovy**: 実行環境
- **Apache PDFBox 2.0.29**: PDFメタデータ処理（自動ダウンロード）

## ライセンス

このツールはオープンソースソフトウェアです。
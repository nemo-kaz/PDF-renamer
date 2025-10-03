
# PDF-renamer

## 概要
PDF-renamerは、PDFファイルのタイトルメタデータを取得し、そのタイトルをファイル名としてリネームするWindows向けツールです。
指定したディレクトリ以下の全PDFファイルを再帰的に探索し、タイトル情報をもとにリネームコマンドをまとめたスクリプトファイル（PowerShell: renamePDFs.ps1）を自動生成します。

## 特徴
- PDFのタイトルメタデータを取得し、ファイル名に反映
- サブディレクトリも含めて再帰的に探索
- Windowsで使えない文字（: / * ? " < > | など）は _ に自動置換
- すべてのリネームコマンドを1つのPowerShellスクリプトにまとめて出力
- 生成ファイルはShift-JIS（MS932）で保存されるため日本語も安全

## 使い方
1. GroovyとApache PDFBoxライブラリが必要です。
2. コマンドラインで以下を実行します。
	```pwsh
	groovy generatePDFRenameIt.groovy <対象ディレクトリ>
	```
	例：
	```pwsh
	groovy generatePDFRenameIt.groovy D:\PROJECT\PDF-renamer\test
	```
3. 実行後、指定ディレクトリ直下に `renamePDFs.ps1` が生成されます（Shift-JIS形式）。
4. PowerShellでスクリプトを実行することで、PDFファイル名がタイトルに基づき一括リネームされます。

## 依存
- Groovy
- Apache PDFBox（@Grabで自動取得）

## 注意事項
- PDFタイトルが空の場合はリネームされません。
- ファイル名に使えない文字は自動で _ に置換されます。
- スクリプトの実行は自己責任で行ってください。

## ライセンス
MIT License


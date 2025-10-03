#!/usr/bin/env groovy

/**
 * PDF リネーミングツール
 * 
 * 概要:
 * 指定されたディレクトリとそのサブディレクトリ内のすべてのPDFファイルを再帰的に検索し、
 * 各PDFファイルのタイトルプロパティに基づいてファイル名を自動的に変更するGroovy CLIコマンドです。
 * 
 * 主な機能:
 * - サブディレクトリの再帰的探索
 * - シンボリックリンクの無視
 * - PDFメタデータからのタイトル抽出
 * - Windowsファイル名無効文字のサニタイズ
 * - 除外条件による処理スキップ
 * - 多言語ファイル名の削除対象識別
 * - UTF-8/Shift-JIS/EUC-JP文字エンコーディング対応
 * - 例外・エラーの適切なハンドリング
 * - renamePDFs.groovyスクリプトの生成
 * 
 * 使用方法:
 * groovy generatePDFRenameIt.groovy [ディレクトリパス] [文字エンコーディング]
 * 
 * 例:
 * groovy generatePDFRenameIt.groovy
 * groovy generatePDFRenameIt.groovy /path/to/pdfs
 * groovy generatePDFRenameIt.groovy /path/to/pdfs UTF-8
 * 
 * @author PDF Renamer Tool
 * @version 1.0
 */

@Grab('org.apache.pdfbox:pdfbox:2.0.29')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.io.OutputStreamWriter
import java.io.FileOutputStream

/**
 * Windowsファイルシステムで無効な文字をアンダースコアに置換
 * 要件5.1-5.4: : " < > | * ? / \ を _ に置換
 * @param name 元のファイル名文字列
 * @return サニタイズされたファイル名文字列
 */
def sanitizeFileName(String name) {
    if (!name || name.trim().isEmpty()) {
        return name
    }
    // Windows無効文字を _ に置換: \ / : * ? " < > |
    return name.replaceAll('[\\\\/:*?"<>|]', '_')
}

/**
 * ファイル名が日本語・英語以外の言語で記述されているかを判定
 * 要件7.6: 日本語・英語以外のファイル名を識別
 * @param name ファイル名文字列
 * @return true=日英以外、false=日英
 */
def isNonJapaneseEnglish(String name) {
    if (!name || name.trim().isEmpty()) {
        return false
    }
    // 日本語（ひらがな、カタカナ、漢字）と英語（ラテン文字）のパターン
    def jpEnPattern = /[\p{InHiragana}\p{InKatakana}\p{InCJK_Unified_Ideographs}\p{IsLatin}\p{IsDigit}\s\-_\.]/
    // ファイル名から拡張子を除いた部分で判定
    def nameWithoutExt = name.replaceAll(/\.[^.]*$/, '')
    return !(nameWithoutExt ==~ /^${jpEnPattern}*$/)
}

/**
 * 文字列を三重引用符で囲む
 * 要件2.4: エラーを防ぐためにパスを三重引用符で囲む
 * @param str 囲む対象の文字列
 * @return 三重引用符で囲まれた文字列
 */
def tripleQuote(String str) {
    return '"""' + str + '"""'
}

/**
 * パス文字列を正規化（バックスラッシュをフォワードスラッシュに変換）
 * 要件6.2: パス文字列でフォワードスラッシュを使用
 * @param path パス文字列
 * @return 正規化されたパス文字列
 */
def normalizePath(String path) {
    return path.replace('\\', '/')
}

/**
 * 指定された文字エンコーディングでファイルライターを作成
 * 要件7.4: UTF-8、Shift-JIS、EUC-JP対応
 * @param path ファイルパス
 * @param encoding 文字エンコーディング
 * @return OutputStreamWriter
 */
def getWriter(String path, String encoding) {
    try {
        return new OutputStreamWriter(new FileOutputStream(path), Charset.forName(encoding))
    } catch (Exception e) {
        println "エンコーディングエラー: ${encoding} -> UTF-8にフォールバック"
        return new OutputStreamWriter(new FileOutputStream(path), Charset.forName('UTF-8'))
    }
}

/**
 * 出力ファイルを初期化し、適切なヘッダーを追加
 * 要件2.1, 2.2: renamePDFs.groovy ファイルの作成と初期化
 */
def initializeOutputFile(writer) {
    writer.write("// PDFファイルリネームスクリプト（自動生成）\n")
    writer.write("// 生成日時: ${new Date()}\n")
    writer.write("import java.nio.file.Files\n")
    writer.write("import java.nio.file.Paths\n")
    writer.write("\n")
}

/**
 * コマンドライン引数を解析し、デフォルト値を設定
 * 要件7.4: ルートディレクトリと文字エンコーディングの引数処理
 */
def parseArguments(args) {
    def rootDir = args.length > 0 ? args[0] : new File('.').absolutePath
    def encoding = args.length > 1 ? args[1] : 'UTF-8'
    
    // ディレクトリの存在確認
    def rootFile = new File(rootDir)
    if (!rootFile.exists() || !rootFile.isDirectory()) {
        println "エラー: 指定されたディレクトリが存在しません: ${rootDir}"
        System.exit(1)
    }
    
    // サポートされているエンコーディングの確認
    def supportedEncodings = ['UTF-8', 'Shift-JIS', 'EUC-JP']
    if (!supportedEncodings.contains(encoding)) {
        println "警告: サポートされていないエンコーディング: ${encoding} -> UTF-8を使用"
        encoding = 'UTF-8'
    }
    
    return [rootDir: rootFile.absolutePath, encoding: encoding]
}

// グローバル変数として定義
def config = parseArguments(args)
def rootDir = config.rootDir
def encoding = config.encoding
def outPath = new File(rootDir + File.separator + 'renamePDFs.groovy').absolutePath
def writer = null

/**
 * PDFファイルからタイトルプロパティを安全に抽出
 * 要件3.1, 3.2: PDFメタデータからタイトル抽出
 * @param file PDFファイル
 * @return タイトル文字列（null の場合もある）
 */
def extractPdfTitle(File file) {
    PDDocument doc = null
    try {
        doc = PDDocument.load(file)
        PDDocumentInformation info = doc.getDocumentInformation()
        return info.getTitle()
    } catch (Exception e) {
        println "PDF処理失敗: ${file.absolutePath} - ${e.getMessage()}"
        return null
    } finally {
        if (doc != null) {
            try {
                doc.close()
            } catch (Exception e) {
                // クローズエラーは無視
            }
        }
    }
}

/**
 * PDFタイトルが除外条件に該当するかを判定
 * 要件4.1-4.4: 除外条件の判定
 * @param title PDFタイトル
 * @return true=除外対象、false=処理対象
 */
def shouldExcludeTitle(String title) {
    if (!title || title.trim().isEmpty()) {
        return true  // 空文字列は除外
    }
    
    def trimmedTitle = title.trim()
    return trimmedTitle.startsWith('_') || 
           trimmedTitle.startsWith('~') || 
           trimmedTitle.startsWith('Untitled')
}

/**
 * 再帰的にディレクトリ探索し、PDFファイルを処理
 */
def processDir(File dir, writer) {
    def files
    try {
        files = dir.listFiles()
        if (files == null) {
            println "アクセス不可: ${dir.absolutePath}"
            return
        }
    } catch (Exception e) {
        println "ディレクトリ取得失敗: ${dir.absolutePath} - ${e.getMessage()}"
        return
    }
    
    // 進行状況表示
    println "処理中: ${dir.absolutePath}"
    
    for (file in files) {
        // シンボリックリンクの検出とスキップ（要件1.5）
        if (file.isDirectory()) {
            if (!java.nio.file.Files.isSymbolicLink(file.toPath())) {
                processDir(file, writer)  // 再帰的にサブディレクトリを処理
            } else {
                println "シンボリックリンクをスキップ: ${file.absolutePath}"
            }
        } else if (file.isFile()) {
            // PDFファイルの識別（要件1.3）
            if (file.name.toLowerCase().endsWith('.pdf')) {
                // 多言語ファイル名判定
                if (isNonJapaneseEnglish(file.name)) {
                    println "削除対象: ${file.absolutePath}"
                    def normalizedPath = normalizePath(file.absolutePath)
                    writer.write("// 削除対象: ${file.name}\n")
                    writer.write("Files.delete(Paths.get(${tripleQuote(normalizedPath)}))\n")
                    writer.write("println \"削除: ${normalizedPath}\"\n")
                    continue
                }
                
                // PDFタイトル抽出
                def title = extractPdfTitle(file)
                if (!title) {
                    println "タイトル取得失敗: ${file.absolutePath}"
                    continue
                }
                
                // 除外条件チェック
                if (shouldExcludeTitle(title)) {
                    println "除外: ${file.absolutePath} -> タイトル: '${title}'"
                    continue
                }
                
                // ファイル名サニタイズ
                def newName = sanitizeFileName(title.trim()) + '.pdf'
                
                // リネームコマンド生成
                def oldPath = normalizePath(file.absolutePath)
                def newPath = normalizePath(file.parentFile.absolutePath) + '/' + newName
                
                writer.write("// ${file.name} -> ${newName}\n")
                writer.write("Files.move(Paths.get(${tripleQuote(oldPath)}), Paths.get(${tripleQuote(newPath)}))\n")
                writer.write("println \"リネーム: ${oldPath} -> ${newPath}\"\n")
                println "リネーム予定: ${file.name} -> ${newName}"
            } else {
                // PDFファイル以外はスキップ
                // println "スキップ: ${file.absolutePath}"
            }
        }
    }
}

/**
 * メイン処理フローの実行
 * 要件1.1, 2.1, 7.5: 統合されたメイン処理
 */
try {
    println "=== PDF リネーミングツール開始 ==="
    println "対象ディレクトリ: ${rootDir}"
    println "文字エンコーディング: ${encoding}"
    println "出力ファイル: ${outPath}"
    println ""
    
    // 出力ファイルの初期化
    writer = getWriter(outPath, encoding)
    initializeOutputFile(writer)
    
    // 処理開始時刻記録
    def startTime = System.currentTimeMillis()
    
    // ディレクトリ処理実行
    processDir(new File(rootDir), writer)
    
    // 処理完了
    def endTime = System.currentTimeMillis()
    def duration = (endTime - startTime) / 1000.0
    
    println ""
    println "=== 処理完了 ==="
    println "実行時間: ${duration} 秒"
    println "生成されたスクリプト: ${outPath}"
    println "スクリプトを実行してファイルをリネームしてください。"
    
} catch (Exception e) {
    println "エラー: メイン処理中に例外が発生しました: ${e.getMessage()}"
    e.printStackTrace()
} finally {
    // リソースのクリーンアップ
    if (writer != null) {
        try {
            writer.close()
        } catch (Exception e) {
            println "警告: ファイルクローズ中にエラーが発生しました: ${e.getMessage()}"
        }
    }
}


@Grab('org.apache.pdfbox:pdfbox:2.0.29')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

/**
 * PDFファイルのタイトルを取得し、ファイル名をリネームするスクリプト
 * - サブディレクトリも再帰的に探索
 * - シンボリックリンクは無視
 * - ファイル名が日本語・英語以外なら削除
 * - 文字コードはUTF-8/Shift-JIS/EUC-JP対応
 * - 例外・エラーは画面表示し継続
 * - renamePDFs.groovyにリネーム処理をまとめて出力
 */

// Windowsで使えない文字を置換
def sanitizeFileName(String name) {
    return name.replaceAll('[\\/:*?"<>|]', '_')
}

// ファイル名が日本語・英語以外ならtrue（簡易判定: Unicodeブロックで判定）
def isNonJapaneseEnglish(String name) {
    def jpEnPattern = /[\\p{InHiragana}\\p{InKatakana}\\p{InCJK_Unified_Ideographs}\\p{IsLatin}]/
    return !(name =~ jpEnPattern)
}

// 三重引用符で囲む関数
def tripleQuote(str) {
    return '"""' + str + '"""'
}

// 文字コード対応（UTF-8, Shift-JIS, EUC-JP）
def getWriter(String path, String encoding) {
    return new OutputStreamWriter(new FileOutputStream(path), Charset.forName(encoding))
}

// リネーム処理を出力するgroovyファイル名
def rootDir = args.length > 0 ? args[0] : new File('.').absolutePath
def outPath = new File(new File(rootDir).absolutePath + File.separator + 'renamePDFs.groovy').absolutePath
def encoding = (args.length > 1) ? args[1] : 'UTF-8' // 2番目の引数で文字コード指定可
def writer = getWriter(outPath, encoding)
writer.write("// PDFファイルリネームスクリプト（自動生成）\n")

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
    for (file in files) {
        // シンボリックリンクは無視
        if (file.isDirectory() && !java.nio.file.Files.isSymbolicLink(file.toPath())) {
            processDir(file, writer)
        } else if (file.isFile() && file.name.toLowerCase().endsWith('.pdf')) {
            // 多言語ファイル名判定
            if (isNonJapaneseEnglish(file.name)) {
                println "削除対象: ${file.absolutePath}"
                writer.write("Files.delete(Paths.get(${tripleQuote(file.absolutePath.replace('\\', '/'))}))\n")
                continue
            }
            try {
                PDDocument doc = PDDocument.load(file)
                PDDocumentInformation info = doc.getDocumentInformation()
                def title = info.getTitle()
                doc.close()
                if (!title || !title.trim()) {
                    println "タイトル空: ${file.absolutePath}"
                    continue
                }
                def newName = sanitizeFileName(title.trim()) + '.pdf'
                // 除外条件
                if (newName.startsWith('_') || newName.startsWith('~') || newName.startsWith('Untitled') || newName == '.pdf') {
                    println "除外: ${file.absolutePath} -> ${newName}"
                    continue
                }
                // リネームコマンド出力（パスは三重引用符で囲み、バックスラッシュはスラッシュに変換）
                def oldPath = file.absolutePath.replace('\\', '/')
                def newPath = file.parentFile.absolutePath.replace('\\', '/') + '/' + newName
                writer.write("def oldPath = Paths.get(${tripleQuote(oldPath)})\n")
                writer.write("def newPath = Paths.get(${tripleQuote(newPath)})\n")
                writer.write("Files.move(Paths.get(${tripleQuote(oldPath)}), Paths.get(${tripleQuote(newPath)}))\n")
                writer.write("println \"リネーム: ${oldPath} -> ${newPath}\"\n")
                println "リネーム: ${file.absolutePath} -> ${newName}"
            } catch (Exception e) {
                println "PDF処理失敗: ${file.absolutePath} - ${e.getMessage()}"
            }
        }
    }
}

// 実行
println "開始: ${rootDir} (encoding=${encoding})"
processDir(new File(rootDir), writer)
writer.close()
println "renamePDFs.groovy を生成しました: ${outPath}"
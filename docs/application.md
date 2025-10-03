
groovy CLIコマンドです。
コマンドが発行されたディレクトリーから再帰的に
subdirectoryを降下していき処理を続けます。
起動されたdirectoryに renamePDFs.groovy というファイルを作り、これを単一の出力ファイルとします。
subdirectoryも含めて、このgroovyにまとめて出力します。
各directoryに入ったら、そのdirectoryにあるすべてのファイルを調べます。
訪問したそのdirectoryにあるすべてのファイル名をフルパス形式でリスト化する
、その中からPDFファイルのみを処理対象とする。

個別のPDFに対して、プロパティを調べる。
タイトルというプロパティに含まれる文字列をそのPDFのファイル名にする置換コマンドを出力対象のgroovyファイルに書き足す。
ただし　タイトル名が "_" または "~" で始まるファイル名の場合は変更処理しない。
また、タイトル名が "Untitled" で始まる場合も変更処理しない。
また、タイトル名が が空文字列の場合も変更処理しない。

renamePDFs.groovy ひとつでsubdirectoryにあるPDF全てをリネームします。
また、: や " などのWindowsのファイル名に使えない文字は _ に置き換える。

動作例
捜査対象ファイルが
d:\ABC\aaa100.pdf　
そしてそのプロパティのタイトルが "こんにちは" であった場合
"こんにちは.pdf" という名前にリネームします Files.move()メソッドを使います
ファイル名は、"""で囲んで、誤動作を予防します

書き方は以下のようになります
def oldPath = Paths.get("d:/ABC/aaa100.pdf")
def newPath = Paths.get("d:/ABC/こんにちは.pdf")
Files.move(oldPath, newPath)
println "リネーム: ${oldPath} -> ${newPath}"


このプログラムを作ってください。

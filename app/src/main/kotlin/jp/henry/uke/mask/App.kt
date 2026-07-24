package jp.henry.uke.mask

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "uke-pseudonymize",
    mixinStandardHelpOptions = true,
    versionProvider = PropertiesVersionProvider::class,
    description = ["UKEファイルを仮名化"],
)
class App : Callable<Int> {
    @Parameters(index = "0", description = ["マスクするUKEファイル"])
    lateinit var input: File

    @Parameters(index = "1", description = ["マスク済みUKEファイル出力先"])
    lateinit var output: Path

    @Option(names = ["--seed"], description = ["乱数シード"])
    var seed: Int = SecureRandom.getInstanceStrong().nextInt()

    @Option(names = ["--rosai"], description = ["労災レセプトとして処理"])
    var rosai: Boolean = false

    override fun call(): Int {
        if (!input.canRead()) {
            System.err.println("UKEファイルが見つかりません: ${input.absolutePath}")
            return 1
        }

        val mode = if (rosai) "労災" else "医科"
        println("マスク処理を開始します（$mode, シード値 $seed）……")
        val engine = MaskingEngine(seed)

        Files.newBufferedWriter(output, CHARSET).use { writer ->
            Files
                .newBufferedReader(input.toPath(), CHARSET)
                .lines()
                .map {
                    if (rosai) {
                        when {
                            it.startsWith("RE") -> engine.maskReRosai(it)
                            it.startsWith("RR") -> engine.maskRr(it)
                            it.startsWith("RS") -> engine.maskRs(it)
                            it.startsWith("IR") -> engine.maskIrRosai(it)
                            it.startsWith("CO") -> engine.maskCo(it)
                            else -> it
                        }
                    } else {
                        when {
                            it.startsWith("RE") -> engine.maskRe(it)
                            it.startsWith("HO") -> engine.maskHo(it)
                            it.startsWith("KO") -> engine.maskKo(it)
                            it.startsWith("IR") -> engine.maskIr(it)
                            else -> it
                        }
                    }
                }.forEach {
                    writer.write(it)
                    writer.newLine()
                }
        }
        println("マスク処理を完了しました。")
        return 0
    }

    companion object {
        val CHARSET: Charset = Charset.forName("MS932")
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(App()).execute(*args))

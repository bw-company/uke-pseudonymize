package jp.henry.uke.mask

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class MaskingEngine(seed: Int, private val clock: Clock) {
    private val random = Random(seed)
    private val names = HashMap<String, HashMap<String, String>>()
    private val numbers = HashMap<Int, HashMap<String, String>>()

    /**
     * レセプト共通レコードをマスクする。[4]の氏名、[36]のカナ氏名をマスクする。
     * @see <a href="https://www.mhlw.go.jp/seisakunitsuite/bunya/kenkou_iryou/iryouhoken/reseputo/pdf/kirokusiyou_1.pdf">オンライン又は光ディスク等による請求に係る記録条件仕様（医科用）</a>
     * @see <a href="https://www.ssk.or.jp/seikyushiharai/rezept/iryokikan/iryokikan_02.files/jiki_i01.pdf">レセプト電算処理システム 電子レセプトの作成手引き</a>
     */
    fun maskRe(line: String): String {
        val split: List<String> = line.split(",")
        // split[6] にアクセスする前に項目数を確認しておく
        require(split.size == 38) {
            "レセプト共通レコードには38項目が含まれるべきですが ${split.size}項目しか見つかりませんでした"
        }

        return split.withIndex().joinToString(",") {
            when (it.index) {
                4 -> {
                    val birthDay = LocalDate.parse(split[6], DateTimeFormatter.ofPattern("yyyyMMdd"))
                    maskPatientName(it.value, birthDay)
                }
//                誕生日は７５歳に到達した日の確認が必要となるため、そのままの値で使用
//                6 -> maskDate(it.value)
//                患者番号・カルテ番号は実データを確認する際に使用することがありそうなため、そのままの値で使用
//                13 -> mask(it.value)
                36 -> maskName("患者カナ", it.value)
                else -> it.value
            }
        }
    }

    fun maskPatientName(raw: String, birthDay: LocalDate): String {
        // 当月の1日時点での年齢を表示する
        val base = LocalDate.ofInstant(clock.instant(), clock.zone).withDayOfMonth(1)
        // 誕生日の前日に年齢を加算する
        val age = ChronoUnit.YEARS.between(birthDay.minusDays(1), base)
        return if (age <= 6) {
            "${maskName("患者名", raw)}（${age}歳, 未就学児）"
        } else {
            "${maskName("患者名", raw)}（${age}歳）"
        }
    }

    /**
     * 保険者レコードをマスクする。[2]の被保険者証（手帳）等の記号、[3]の被保険者証（手帳）等の番号、
     * [9]の証明書番号をマスクする。
     * @see <a href="https://www.mhlw.go.jp/seisakunitsuite/bunya/kenkou_iryou/iryouhoken/reseputo/pdf/kirokusiyou_1.pdf">オンライン又は光ディスク等による請求に係る記録条件仕様（医科用）</a>
     * @see <a href="https://www.ssk.or.jp/seikyushiharai/rezept/iryokikan/iryokikan_02.files/jiki_i01.pdf">レセプト電算処理システム 電子レセプトの作成手引き</a>
     */
    fun maskHo(line: String): String =
        line.split(",").withIndex().joinToString(",") {
            when (it.index) {
                2 -> maskNumber(it.value, 40)
                3 -> maskNumber(it.value, 40)
                9 -> maskNumber(it.value, 3)
                else -> it.value
            }
        }

    /**
     * 公費レコードをマスクする。[2]の受給者番号をマスクする。
     * @see <a href="https://www.mhlw.go.jp/seisakunitsuite/bunya/kenkou_iryou/iryouhoken/reseputo/pdf/kirokusiyou_1.pdf">オンライン又は光ディスク等による請求に係る記録条件仕様（医科用）</a>
     * @see <a href="https://www.ssk.or.jp/seikyushiharai/rezept/iryokikan/iryokikan_02.files/jiki_i01.pdf">レセプト電算処理システム 電子レセプトの作成手引き</a>
     */
    fun maskKo(line: String): String =
        line.split(",").withIndex().joinToString(",") {
            when (it.index) {
                2 -> maskNumber(it.value, 7)
                else -> it.value
            }
        }

    /**
     * 医療機関情報レコードをマスクする。[4]の医療機関コード、[6]の医療機関名称、[9]の電話番号をマスクする。
     * @see <a href="https://www.mhlw.go.jp/seisakunitsuite/bunya/kenkou_iryou/iryouhoken/reseputo/pdf/kirokusiyou_1.pdf">オンライン又は光ディスク等による請求に係る記録条件仕様（医科用）</a>
     * @see <a href="https://www.ssk.or.jp/seikyushiharai/rezept/iryokikan/iryokikan_02.files/jiki_i01.pdf">レセプト電算処理システム 電子レセプトの作成手引き</a>
     */
    fun maskIr(line: String): String =
        line.split(",").withIndex().joinToString(",") {
            when (it.index) {
                4 -> maskNumber(it.value, 7)
                6 -> maskName("医療機関名", it.value)
                9 -> maskTelNum(it.value)
                else -> it.value
            }
        }

    fun maskName(prefix: String, name: String) = names.getOrPut(prefix) {
        HashMap()
    }.getOrPut(name) {
        "$prefix${random.nextInt(Integer.MAX_VALUE)}"
    }

    fun maskNumber(text: String, length: Int): String = numbers.getOrPut(length) {
        HashMap()
    }.getOrPut(text) {
        (1..length).joinToString("") {
            random.nextInt(10).toString()
        }
    }

    private fun maskTelNum(text: String) = "000-0000-0000"
}

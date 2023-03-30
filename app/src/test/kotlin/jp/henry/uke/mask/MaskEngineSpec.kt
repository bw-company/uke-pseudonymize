package jp.henry.uke.mask

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.LocalDate

class MaskEngineSpec : DescribeSpec({
    describe("maskNumber") {
        it("generates number with given length") {
            checkAll(Arb.int(), Arb.string(), Arb.int(1, 10)) { seed, text, length ->
                MaskingEngine(seed).maskNumber(text, length).length shouldBe length
            }
        }
    }
    describe("maskName") {
        it("generates the same name for the same input") {
            checkAll<Int, String, String> { seed, prefix, text ->
                MaskingEngine(seed).maskName(prefix, text) shouldBe MaskingEngine(seed).maskName(prefix, text)
            }
        }
    }
    describe("maskPatientName") {
        it("computes age and embeds it into the masked name") {
            val today = LocalDate.of(2020, 3, 1)
            val birthDay = LocalDate.of(2010, 2, 1)

            MaskingEngine(0).maskPatientName("患者", today, birthDay) shouldContain "10歳"
        }
        describe("誕生日の前日に年齢を加算する") {
            // TODO うるう年の3月1日（誕生日の前日が2月29日）のケースをテスト
            it("誕生日が1日の場合") {
                val birthDay = LocalDate.of(2020, 2, 1)

                val theDayBefore = LocalDate.of(2021, 1, 30)
                val endOfTheLastMonth = LocalDate.of(2021, 1, 31)
                val beginningOfThisMonth = LocalDate.of(2021, 2, 1)

                MaskingEngine(0).maskPatientName("患者", theDayBefore, birthDay) shouldContain "（0歳"
                // この時点では1歳だが、その月の1日での年齢を表示するため変更なし
                MaskingEngine(0).maskPatientName("患者", endOfTheLastMonth, birthDay) shouldContain "（0歳"
                MaskingEngine(0).maskPatientName("患者", beginningOfThisMonth, birthDay) shouldContain "（1歳"
            }
            it("誕生日が2日の場合") {
                val birthDay = LocalDate.of(2020, 2, 2)

                val endOfTheLastMonth = LocalDate.of(2021, 1, 31)
                val beginningOfThisMonth = LocalDate.of(2021, 2, 1)
                val endOfTheMonth = LocalDate.of(2021, 2, 28)
                val beginningOfTheNextMonth = LocalDate.of(2021, 3, 1)

                MaskingEngine(0).maskPatientName("患者", endOfTheLastMonth, birthDay) shouldContain "（0歳"

                // この時点で1歳となり、その月の1日での年齢を表示するため1歳と表示
                MaskingEngine(0).maskPatientName("患者", beginningOfThisMonth, birthDay) shouldContain "（1歳"

                MaskingEngine(0).maskPatientName("患者", endOfTheMonth, birthDay) shouldContain "（1歳"
                MaskingEngine(0).maskPatientName("患者", beginningOfTheNextMonth, birthDay) shouldContain "（1歳"
            }
            it("誕生日がそれ以外の場合") {
                checkAll(Arb.int(3, 31)) {
                    val birthDay = LocalDate.of(2019, 3, it)

                    val twoDaysBefore = LocalDate.of(2020, 3, it - 2)
                    val oneDayBefore = LocalDate.of(2020, 3, it - 1)
                    val endOfTheMonth = LocalDate.of(2020, 3, 31)
                    val beginningOfTheNextMonth = LocalDate.of(2020, 4, 1)

                    MaskingEngine(0).maskPatientName("患者", twoDaysBefore, birthDay) shouldContain "（0歳"

                    // その月の1日での年齢を表示するため、誕生日前日でも変更なし
                    MaskingEngine(0).maskPatientName("患者", oneDayBefore, birthDay) shouldContain "（0歳"

                    // 次の月から年齢が加算される
                    MaskingEngine(0).maskPatientName("患者", endOfTheMonth, birthDay) shouldContain "（0歳"
                    MaskingEngine(0).maskPatientName("患者", beginningOfTheNextMonth, birthDay) shouldContain "（1歳"
                }
            }
        }
        describe("未就学児判定") {
            it("0-6歳ならば未就学児であることを表示") {
                checkAll(Arb.int(0, 6)) {
                    val birthDay = LocalDate.of(2000, 1, 2)
                    val thisMonth = LocalDate.of(2000 + it, 1, 2)

                    MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldContain ", 未就学児）"
                }
            }
            it("7歳以上であれば表示しない") {
                checkAll(Arb.int(7, 120)) {
                    val birthDay = LocalDate.of(2000, 1, 2)
                    val thisMonth = LocalDate.of(2000 + it, 1, 1)

                    MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldNotContain ", 未就学児）"
                }
            }
        }
        describe("75歳到達月判定") {
            it("月末時点で75歳0ヶ月であれば「75歳到達月」と表示") {
                checkAll(Arb.int(1, 31)) {
                    val birthDay = LocalDate.of(1955, 1, 2)
                    val thisMonth = LocalDate.of(2030, 1, it)

                    MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldContain ", 75歳到達月"
                }
            }
            it("月末時点で74歳11ヶ月あるいは75歳1ヶ月であれば表示しない") {
                val birthDay = LocalDate.of(1955, 1, 2)
                val before = LocalDate.of(2029, 12, 31)
                val after = LocalDate.of(2030, 2, 1)

                MaskingEngine(0).maskPatientName("患者", before, birthDay) shouldNotContain ", 75歳到達月"
                MaskingEngine(0).maskPatientName("患者", after, birthDay) shouldNotContain ", 75歳到達月"
            }
            it("当月1日が誕生日の場合は到達月ではない") {
                checkAll(Arb.int(1, 28)) {
                    val birthDay = LocalDate.of(1955, 2, 1)
                    val thisMonth = LocalDate.of(2030, 2, it)

                    MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldNotContain ", 75歳到達月"
                }
            }
        }
    }
})

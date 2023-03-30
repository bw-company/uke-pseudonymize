package jp.henry.uke.mask

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MaskEngineSpec : DescribeSpec({
    describe("maskNumber") {
        it("generates number with given length") {
            checkAll(Arb.int(), Arb.string(), Arb.int(1, 10)) { seed, text, length ->
                MaskingEngine(seed, Clock.systemUTC()).maskNumber(text, length).length shouldBe length
            }
        }
    }
    describe("maskName") {
        it("generates the same name for the same input") {
            checkAll<Int, String, String> { seed, prefix, text ->
                MaskingEngine(seed, Clock.systemUTC()).maskName(prefix, text) shouldBe MaskingEngine(seed, Clock.systemUTC()).maskName(prefix, text)
            }
        }
    }
    describe("maskPatientName") {
        it("computes age and embeds it into the masked name") {
            val today = Clock.fixed(Instant.parse("2020-03-01T09:00:00.00Z"), zoneId)
            val birthDay = LocalDate.of(2010, 2, 1)

            MaskingEngine(0, today).maskPatientName("患者", birthDay) shouldContain "10歳"
        }
        describe("誕生日の前日に年齢を加算する") {
            it("誕生日が1日の場合") {
                val birthDay = LocalDate.of(2020, 2, 1)

                val theDayBefore = Clock.fixed(Instant.parse("2021-01-30T09:00:00.00Z"), zoneId)
                val endOfTheLastMonth = Clock.fixed(Instant.parse("2021-01-31T09:00:00.00Z"), zoneId)
                val beginningOfThisMonth = Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), zoneId)

                MaskingEngine(0, theDayBefore).maskPatientName("患者", birthDay) shouldContain "（0歳"
                // この時点では1歳だが、その月の1日での年齢を表示するため変更なし
                MaskingEngine(0, endOfTheLastMonth).maskPatientName("患者", birthDay) shouldContain "（0歳"
                MaskingEngine(0, beginningOfThisMonth).maskPatientName("患者", birthDay) shouldContain "（1歳"
            }
            it("誕生日が2日の場合") {
                val birthDay = LocalDate.of(2020, 2, 2)

                val endOfTheLastMonth = Clock.fixed(Instant.parse("2021-01-31T09:00:00.00Z"), zoneId)
                val beginningOfThisMonth = Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), zoneId)
                val endOfTheMonth = Clock.fixed(Instant.parse("2021-02-28T09:00:00.00Z"), zoneId)
                val beginningOfTheNextMonth = Clock.fixed(Instant.parse("2021-03-01T09:00:00.00Z"), zoneId)

                MaskingEngine(0, endOfTheLastMonth).maskPatientName("患者", birthDay) shouldContain "（0歳"

                // この時点で1歳となり、その月の1日での年齢を表示するため1歳と表示
                MaskingEngine(0, beginningOfThisMonth).maskPatientName("患者", birthDay) shouldContain "（1歳"

                MaskingEngine(0, endOfTheMonth).maskPatientName("患者", birthDay) shouldContain "（1歳"
                MaskingEngine(0, beginningOfTheNextMonth).maskPatientName("患者", birthDay) shouldContain "（1歳"
            }
            it("誕生日がそれ以外の場合") {
                checkAll(Arb.int(3, 31)) {
                    val birthDay = LocalDate.of(2019, 3, it)

                    val twoDaysBefore = Clock.fixed(Instant.parse("2020-03-${"%02d".format(it - 2)}T09:00:00.00Z"), zoneId)
                    val oneDayBefore = Clock.fixed(Instant.parse("2020-03-${"%02d".format(it - 1)}T09:00:00.00Z"), zoneId)
                    val endOfTheMonth = Clock.fixed(Instant.parse("2020-03-31T09:00:00.00Z"), zoneId)
                    val beginningOfTheNextMonth = Clock.fixed(Instant.parse("2020-04-01T09:00:00.00Z"), zoneId)

                    MaskingEngine(0, twoDaysBefore).maskPatientName("患者", birthDay) shouldContain "（0歳"

                    // その月の1日での年齢を表示するため、誕生日前日でも変更なし
                    MaskingEngine(0, oneDayBefore).maskPatientName("患者", birthDay) shouldContain "（0歳"

                    // 次の月から年齢が加算される
                    MaskingEngine(0, endOfTheMonth).maskPatientName("患者", birthDay) shouldContain "（0歳"
                    MaskingEngine(0, beginningOfTheNextMonth).maskPatientName("患者", birthDay) shouldContain "（1歳"
                }
            }
        }
        describe("未就学児判定") {
            it("0-6歳ならば未就学児であることを表示") {
                checkAll(Arb.int(0, 6)) {
                    val birthDay = LocalDate.of(2000, 1, 2)
                    val thisMonth = Clock.fixed(Instant.parse("${2000 + it}-01-01T09:00:00.00Z"), zoneId)

                    MaskingEngine(0, thisMonth).maskPatientName("患者", birthDay) shouldContain ", 未就学児）"
                }
            }
            it("7歳以上であれば表示しない") {
                checkAll(Arb.int(7, 120)) {
                    val birthDay = LocalDate.of(2000, 1, 2)
                    val thisMonth = Clock.fixed(Instant.parse("${2000 + it}-01-01T09:00:00.00Z"), zoneId)

                    MaskingEngine(0, thisMonth).maskPatientName("患者", birthDay) shouldNotContain ", 未就学児）"
                }
            }
        }
    }
}) {
    companion object {
        val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}

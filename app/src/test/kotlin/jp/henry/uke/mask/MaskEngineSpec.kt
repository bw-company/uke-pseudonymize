package jp.henry.uke.mask

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
                val today = Clock.fixed(Instant.parse("2021-01-31T09:00:00.00Z"), zoneId)
                LocalDate.ofInstant(today.instant(), zoneId) shouldBe LocalDate.of(2021, 1, 31)

                val birthDay = LocalDate.of(2020, 2, 1)
                MaskingEngine(0, today).maskPatientName("患者", birthDay) shouldContain "（1歳）"
            }
            it("誕生日が1日の場合(2月)") {
                val endOfTheFebEve = Clock.fixed(Instant.parse("2021-02-27T09:00:00.00Z"), zoneId)
                val endOfTheFeb = Clock.fixed(Instant.parse("2021-02-28T09:00:00.00Z"), zoneId)
                val birthDay = LocalDate.of(2020, 3, 1)

                MaskingEngine(0, endOfTheFebEve).maskPatientName("患者", birthDay) shouldContain "（0歳）"
                MaskingEngine(0, endOfTheFeb).maskPatientName("患者", birthDay) shouldContain "（1歳）"
            }
            it("誕生日が1日の場合(うるう年)") {
                val birthDay = LocalDate.of(2019, 3, 1)
                val endOfTheFebEve = Clock.fixed(Instant.parse("2020-02-28T09:00:00.00Z"), zoneId)
                val endOfTheFeb = Clock.fixed(Instant.parse("2020-02-29T09:00:00.00Z"), zoneId)

                MaskingEngine(0, endOfTheFebEve).maskPatientName("患者", birthDay) shouldContain "（0歳）"
                MaskingEngine(0, endOfTheFeb).maskPatientName("患者", birthDay) shouldContain "（1歳）"
            }
            it("誕生日がそれ以外の場合") {
                val birthDay = LocalDate.of(2019, 3, 3)
                val twoDaysBefore = Clock.fixed(Instant.parse("2020-03-01T09:00:00.00Z"), zoneId)
                val oneDayBefore = Clock.fixed(Instant.parse("2020-03-02T09:00:00.00Z"), zoneId)
                val endOfTheMonth = Clock.fixed(Instant.parse("2020-03-31T09:00:00.00Z"), zoneId)
                val beginningOfTheNextMonth = Clock.fixed(Instant.parse("2020-04-01T09:00:00.00Z"), zoneId)

                MaskingEngine(0, twoDaysBefore).maskPatientName("患者", birthDay) shouldContain "（0歳）"

                // その月の1日での年齢を表示するため変更なし
                MaskingEngine(0, oneDayBefore).maskPatientName("患者", birthDay) shouldContain "（0歳）"

                // 次の月から年齢が加算される
                MaskingEngine(0, endOfTheMonth).maskPatientName("患者", birthDay) shouldContain "（0歳）"
                MaskingEngine(0, beginningOfTheNextMonth).maskPatientName("患者", birthDay) shouldContain "（1歳）"
            }
        }
    }
}) {
    companion object {
        val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
    }
}

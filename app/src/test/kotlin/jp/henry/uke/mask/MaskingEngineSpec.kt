package jp.henry.uke.mask

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import jp.henry.uke.mask.MaskingEngine.Companion.computeAge
import java.nio.charset.Charset
import java.time.LocalDate

class MaskingEngineSpec :
    DescribeSpec({
        describe("computeAge") {
            context("3月1日に生まれた子供") {
                val birthDay = LocalDate.of(2014, 3, 1)
                it("翌年の3月1日に1歳になる") {
                    computeAge(birthDay, LocalDate.of(2015, 2, 28)) shouldBe 0
                    computeAge(birthDay, LocalDate.of(2015, 3, 1)) shouldBe 1
                }
            }
            context("2月29日に生まれた子供") {
                val birthDay = LocalDate.of(2016, 2, 29)
                it("翌年の2月27日はまだ0歳のまま") {
                    computeAge(birthDay, LocalDate.of(2017, 2, 27)) shouldBe 0
                }
                it("翌年の2月28日から3月1日になる瞬間に1歳になる") {
                    computeAge(birthDay, LocalDate.of(2017, 2, 28)) shouldBe 0
                    computeAge(birthDay, LocalDate.of(2017, 3, 1)) shouldBe 1
                }
            }
        }
        describe("maskNumber") {
            it("generates number with given length") {
                checkAll(Arb.int(), Arb.string(), Arb.int(1, 10)) { seed, text, length ->
                    MaskingEngine(seed).maskNumber(text, length).length shouldBe length
                }
            }
        }
        describe("maskName") {
            it("同じ入力に対しては常に同じ結果を返す") {
                checkAll<Int, String, String> { seed, prefix, text ->
                    MaskingEngine(seed).maskName(prefix, text) shouldBe MaskingEngine(seed).maskName(prefix, text)
                }
            }
        }
        describe("maskPatientName") {
            it("満年齢を計算して匿名化された患者名に埋め込む") {
                val today = LocalDate.of(2020, 3, 1)
                val birthDay = LocalDate.of(2010, 2, 1)

                MaskingEngine(0).maskPatientName("患者", today, birthDay) shouldContain "10歳"
            }
            context("誕生日の前日に年齢を加算する") {
                // TODO うるう年の3月1日（誕生日の前日が2月29日）のケースをテスト
                it("誕生日が1日の場合") {
                    val birthDay = LocalDate.of(2020, 2, 1)

                    val theDayBefore = LocalDate.of(2021, 1, 30)
                    val endOfTheLastMonth = LocalDate.of(2021, 1, 31)
                    val beginningOfThisMonth = LocalDate.of(2021, 2, 1)

                    MaskingEngine(0).maskPatientName("患者", theDayBefore, birthDay) shouldContain "(0歳"
                    MaskingEngine(0).maskPatientName("患者", endOfTheLastMonth, birthDay) shouldContain "(0歳"
                    MaskingEngine(0).maskPatientName("患者", beginningOfThisMonth, birthDay) shouldContain "(1歳"
                }
                it("誕生日がそれ以外の場合") {
                    checkAll(Arb.int(2, 31)) {
                        val birthDay = LocalDate.of(2019, 3, it)
                        val today = LocalDate.of(2020, 3, it)
                        val oneDayBefore = LocalDate.of(2020, 3, it - 1)
                        val endOfTheMonth = LocalDate.of(2020, 3, 31)
                        val beginningOfTheNextMonth = LocalDate.of(2020, 4, 1)

                        MaskingEngine(0).maskPatientName("患者", oneDayBefore, birthDay) shouldContain "(0歳"
                        MaskingEngine(0).maskPatientName("患者", today, birthDay) shouldContain "(0歳"
                        MaskingEngine(0).maskPatientName("患者", endOfTheMonth, birthDay) shouldContain "(0歳"

                        // 次の月から年齢が加算される
                        MaskingEngine(0).maskPatientName("患者", beginningOfTheNextMonth, birthDay) shouldContain "(1歳"
                    }
                }
            }
            describe("未就学児判定") {
                it("0-5歳ならば未就学児であることを表示") {
                    checkAll(Arb.int(0, 5)) {
                        val birthDay = LocalDate.of(2000, 1, 2)
                        val thisMonth = LocalDate.of(2000 + it, 1, 2)

                        MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldContain "未就学児"
                    }
                }
                context("6歳の場合") {
                    context("4月1日生まれの場合") {
                        val birthDay = LocalDate.of(2000, 4, 1)
                        it("6歳になる前日の3月31日まで未就学児") {
                            checkAll(Arb.localDate(LocalDate.of(2000, 4, 1), LocalDate.of(2006, 3, 31))) { today ->
                                MaskingEngine(0).maskPatientName("患者", today, birthDay) shouldContain "未就学児"
                            }
                        }
                        it("6歳になる4月1日から就学児") {
                            MaskingEngine(0).maskPatientName("患者", LocalDate.of(2006, 4, 1), birthDay) shouldNotContain "未就学児"
                        }
                    }
                    context("その他の誕生日の場合") {
                        describe("4月2日～3月31日の間に6歳に到達してから、次の3月31日を迎えるまで") {
                            it("未就学児であることを表示") {
                                // うるう年をテストするために2016年2月29日を含むケースを用いる
                                checkAll(Arb.localDate(LocalDate.of(2015, 4, 2), LocalDate.of(2016, 3, 31))) { birthDay ->
                                    // 6歳になる日から確認を開始
                                    var today =
                                        if (birthDay == LocalDate.of(2016, 2, 29)) {
                                            birthDay.plusYears(6).plusDays(1)
                                        } else {
                                            birthDay.plusYears(6)
                                        }
                                    withClue("$birthDay に生まれた子供は $today から6歳になる") {
                                        computeAge(birthDay, today) shouldBe 6
                                    }
                                    val engine = MaskingEngine(0)
                                    do {
                                        engine.maskPatientName("患者", today, birthDay) shouldContain "未就学児"
                                        today = today.plusDays(1)
                                    } while (today <= LocalDate.of(2022, 3, 31))
                                }
                                // うるう年をテストするために2020年2月29日に6歳になるケースも用いる
                                checkAll(Arb.localDate(LocalDate.of(2013, 4, 2), LocalDate.of(2014, 3, 31))) { birthDay ->
                                    // 6歳になる日から確認を開始
                                    var today = birthDay.plusYears(6)
                                    withClue("$birthDay に生まれた子供は $today には6歳になっている") {
                                        computeAge(birthDay, today) shouldBe 6
                                    }
                                    val engine = MaskingEngine(0)
                                    do {
                                        engine.maskPatientName("患者", today, birthDay) shouldContain "未就学児"
                                        today = today.plusDays(1)
                                    } while (today <= LocalDate.of(2020, 3, 31))
                                }
                            }
                        }
                        describe("4月1日から7歳の誕生日を迎えるまで") {
                            it("未就学児であることを表示しない") {
                                checkAll(Arb.localDate(LocalDate.of(2015, 4, 2), LocalDate.of(2016, 4, 1))) { birthDay ->
                                    var today = LocalDate.of(2022, 4, 1)
                                    withClue("$birthDay に生まれた子供は $today にはまだ6歳である") {
                                        computeAge(birthDay, today) shouldBe 6
                                    }
                                    val engine = MaskingEngine(0)
                                    do {
                                        engine.maskPatientName("患者", today, birthDay) shouldNotContain "未就学児"
                                        today = today.plusDays(1)
                                    } while (computeAge(birthDay, today) == 6)
                                }
                            }
                        }
                    }
                    context("7歳以上の場合") {
                        it("就学児だと判定する") {
                            checkAll(Arb.int(7, 120)) {
                                val birthDay = LocalDate.of(2000, 1, 1)
                                val today = LocalDate.of(2000 + it, 1, 1)
                                withClue("$birthDay に生まれた子供は $today にはもう7歳以上である") {
                                    computeAge(birthDay, today) shouldBeGreaterThanOrEqual 7
                                }

                                MaskingEngine(0).maskPatientName("患者", today, birthDay) shouldNotContain "未就学児"
                            }
                        }
                    }
                }
            }
            describe("75歳到達月判定") {
                it("月末時点で75歳0ヶ月であれば「75歳到達月」と表示") {
                    checkAll(Arb.int(1, 31)) {
                        val birthDay = LocalDate.of(1955, 1, 2)
                        val thisMonth = LocalDate.of(2030, 1, it)

                        MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldContain "75歳到達月"
                    }
                }
                it("月末時点で74歳11ヶ月あるいは75歳1ヶ月であれば表示しない") {
                    val birthDay = LocalDate.of(1955, 1, 2)
                    val before = LocalDate.of(2029, 12, 31)
                    val after = LocalDate.of(2030, 2, 1)

                    MaskingEngine(0).maskPatientName("患者", before, birthDay) shouldNotContain "75歳到達月"
                    MaskingEngine(0).maskPatientName("患者", after, birthDay) shouldNotContain "75歳到達月"
                }
                context("当月1日が誕生日の場合") {
                    it("75歳到達月特例にあてはまらないことを表示する") {
                        checkAll(Arb.int(1, 12)) { month ->
                            val birthDay = LocalDate.of(1955, month, 1)
                            checkAll(Arb.int(1, birthDay.lengthOfMonth())) { day ->
                                val thisMonth = LocalDate.of(2030, month, day)
                                MaskingEngine(0).maskPatientName("患者", thisMonth, birthDay) shouldContain "75歳到達月特例対象外"
                            }
                        }
                    }
                }
            }

            /**
             * Shift-JISにおける文字数を計算する。全角は2文字、半角は1文字として数える。
             */
            fun String.countSjisChars(): Int {
                val charset = Charset.forName("SJIS")
                return this.toByteArray(charset).size
            }
            it("半角で40文字、全角で20文字を超えない文字列を生成する") {
                checkAll(Arb.int(), Arb.string(), Arb.int(1, 80)) { seed, name, age ->
                    val birthDay = LocalDate.of(1930, 1, 1)
                    val today = LocalDate.of(1930 + age, 1, 1)
                    val maskedName = MaskingEngine(seed).maskPatientName(name, today, birthDay)
                    val sjisLength = maskedName.countSjisChars()
                    withClue("生成された文字列（$maskedName）の長さが半角で40文字を超えるべきではないが $sjisLength 文字となった") {
                        sjisLength shouldBeLessThanOrEqual 40
                    }
                }
            }
        }
    })

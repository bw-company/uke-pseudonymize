package jp.henry.uke.mask

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Clock

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
})

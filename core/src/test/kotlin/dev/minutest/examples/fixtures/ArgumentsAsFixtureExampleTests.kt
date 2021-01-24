package dev.minutest.examples.fixtures

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import dev.minutest.test2
import org.junit.jupiter.api.Assertions.assertEquals

class ArgumentsAsFixtureExampleTests : JUnit5Minutests {

    data class Arguments(val l: Int, val r: Int)

    fun tests() = rootContext<Arguments> {

        context("positive positive") {
            fixture {
                Arguments(l = 3, r = 1)
            }
            test2("addition") {
                assertEquals(4, l + r)
            }
            test2("subtraction") {
                assertEquals(2, l - r)
            }
        }

        context("positive negative") {
            fixture {
                Arguments(l = 3, r = -1)
            }
            test2("addition") {
                assertEquals(2, l + r)
            }
            test2("subtraction") {
                assertEquals(4, l - r)
            }
        }
    }
}
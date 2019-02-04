package uk.org.minutest

import org.junit.jupiter.api.Assertions.assertEquals
import uk.org.minutest.junit.JUnit5Minutests


class DynamicTests : JUnit5Minutests {

    data class Fixture(
        var fruit: String,
        val log: MutableList<String> = mutableListOf()
    )

    override val tests = rootContext<Fixture> {
        fixture { Fixture("banana") }

        context("same fixture for each") {
            (1..3).forEach { i ->
                test("test for $i") {}
            }
        }

        context("modify fixture for each test") {
            (1..3).forEach { i ->
                context("banana count $i") {
                    deriveFixture { Fixture("$i ${fruit}") }
                    test("test for $i") {
                        assertEquals("$i banana", fruit)
                    }
                }
            }
        }
    }
}
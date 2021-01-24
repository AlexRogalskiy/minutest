package dev.minutest.examples.fixtures

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import dev.minutest.test2
import org.junit.jupiter.api.Assertions.assertEquals


class ParentFixtureExampleTests : JUnit5Minutests {

    data class Fixture(var fruit: String)

    fun tests() = rootContext<Fixture> {
        fixture {
            Fixture("banana")
        }

        test2("sees the context's fixture") {
            assertEquals("banana", fruit)
        }

        context("context inherits fixture") {
            test2("sees the parent context's fixture") {
                assertEquals("banana", fruit)
            }
        }

        context("context replaces fixture") {
            fixture {
                Fixture("kumquat")
            }
            test2("sees the replaced fixture") {
                assertEquals("kumquat", fruit)
            }
        }

        context("context modifies fixture") {
            before {
                fruit = "apple"
            }
            test2("sees the modified fixture") {
                assertEquals("apple", fruit)
            }
        }
    }
}

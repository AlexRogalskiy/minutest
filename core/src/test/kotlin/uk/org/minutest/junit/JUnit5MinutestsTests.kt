package uk.org.minutest.junit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import uk.org.minutest.rootContext
import java.util.*


class JUnit5TestsWithFunction : JUnit5Minutests {

    fun myTests() = rootContext<String> {
        fixture { "banana" }

        test("test") {
            assertEquals("banana", this)
        }
    }
}

class JUnit5TestsWithPlainFixture : JUnit5Minutests {

    override val tests = rootContext<String> {
        fixture { "banana" }

        test("test") {
            assertEquals("banana", this)
        }
    }
}

class JUnit5TestsWithGenericFixture : JUnit5Minutests {

    override val tests = rootContext<Stack<String>> {
        fixture { Stack() }

        test("test") {
            assertTrue(this.isEmpty())
        }
    }
}

class JUnit5TestsWithNullableFixture : JUnit5Minutests {

    override val tests = rootContext<String?> {
        fixture { "banana" }

        test("test") {
            val copy: String = this ?: "kumquat"
            assertEquals("banana", copy)
        }
    }
}
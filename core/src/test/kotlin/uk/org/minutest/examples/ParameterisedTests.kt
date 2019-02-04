package uk.org.minutest.examples

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import uk.org.minutest.junit.JUnit5Minutests
import uk.org.minutest.rootContext

class ParameterisedTests : JUnit5Minutests {

    override val tests = rootContext<Unit> {

        // Once we are in a context, running the same tests for multiple parameters is
        // as easy as calling `test()` for each one.
        listOf("a", "oo", "racecar", "able was I ere I saw elba").forEach { candidate ->
            test("$candidate is a palindrome") {
                assertTrue(candidate.isPalindrome())
            }
        }

        listOf("", "ab", "a man a plan a canal pananma").forEach { candidate ->
            test("$candidate is not a palindrome") {
                assertFalse(candidate.isPalindrome())
            }
        }
    }
}

fun String.isPalindrome(): Boolean =
    if (length == 0) false
    else (0 until length / 2).find { index -> this[index] != this[length - index - 1] } == null
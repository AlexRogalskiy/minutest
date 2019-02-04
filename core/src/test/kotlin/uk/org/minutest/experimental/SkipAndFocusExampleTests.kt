package uk.org.minutest.experimental

import uk.org.minutest.junit.JUnit5Minutests
import uk.org.minutest.rootContext
import kotlin.test.fail


class SkipAndFocusExampleTests : JUnit5Minutests {

    override val tests = rootContext<Unit> {

        // Apply the FOCUS annotation to a test
        FOCUS - test("this test is focused, only other focused things will be run") {}

        context("not focused, so won't be run") {
            test("would fail if the context was run") {
                fail("should not have run")
            }
        }

        context("contains a focused thing, so is run") {

            test("isn't focused, so doesn't run") {
                fail("should not have run")
            }

            FOCUS - context("focused, so will be run") {

                test("this runs") {}

                // apply the SKIP annotation to not run whatever
                SKIP - test("skip overrides the focus") {
                    fail("should not have run")
                }

                SKIP - context("also applies to context") {
                    test("will not be run") {
                        fail("should not have run")
                    }
                }
            }
        }
    }
}
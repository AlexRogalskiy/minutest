package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.*

/**
 * The TestExecutor is built by running down the context tree. It can then run a test by asking the contexts up the
 * tree to supply their fixture.
 */
interface TestExecutor<F> : TestDescriptor {

    fun runTest(runtimeTest: RuntimeTest<F>) {
        runTest(runtimeTest, this.andThenJust(runtimeTest.name))
    }

    fun runTest(test: Test<F>, testDescriptor: TestDescriptor)

    fun <G> andThen(nextContext: RuntimeContext<F, G>): TestExecutor<G> = object: TestExecutor<G> {
        override val name = nextContext.name
        override val parent = this@TestExecutor

        override fun runTest(test: Test<G>, testDescriptor: TestDescriptor) {
            // NB use the top testDescriptor so that we always see the longest path - the one from the
            // bottom of the stack.
            val testForParent: Test<F> = { fixture, _ ->
                nextContext.runTest(test, fixture, testDescriptor)
                fixture
            }
            return parent.runTest(testForParent, testDescriptor)
        }
    }
}

internal object RootExecutor : TestExecutor<Unit>, RootDescriptor {
    override val name = ""
    override val parent: Nothing? = null
    override fun runTest(test: Test<Unit>, testDescriptor: TestDescriptor): Unit = test(Unit, testDescriptor)
}

private fun TestExecutor<*>.andThenJust(name: String): TestDescriptor = object : TestDescriptor {
    override val name: String = name
    override val parent = this@andThenJust
}

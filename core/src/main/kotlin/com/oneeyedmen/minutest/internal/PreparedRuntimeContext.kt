package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.*

/**
 * The runtime representation of a context.
 */
internal data class PreparedRuntimeContext<PF, F> (
    override val name: String,
    override val children: List<RuntimeNode<F>>,
    private val befores: List<(F) -> Unit>,
    private val afters: List<(F) -> Unit>,
    private var afterAlls: List<() -> Unit>,
    private val transforms: List<TestTransform<F>>,
    private val fixtureFactory: (PF, TestDescriptor) -> F,
    override val properties: Map<Any, Any>
) : RuntimeContext<PF, F>() {

    override fun runTest(test: Test<F>, parentFixture: PF, testDescriptor: TestDescriptor): F {
        val testWithPreparedFixture: Test<F> = { parentFixture1, testDescriptor1 ->
            applyBeforesTo(parentFixture1)
                .tryMap { f -> test(f, testDescriptor1) }
                .onLastValue(::applyAftersTo)
                .orThrow()
        }
        val transformedTest = applyTransformsTo(testWithPreparedFixture)
        return transformedTest.invoke(fixtureFactory(parentFixture, testDescriptor), testDescriptor)
    }

    override fun close() {
        afterAlls.forEach {
            it()
        }
    }

    private fun applyTransformsTo(test: Test<F>): Test<F> =
        transforms.fold(test) { acc, transform -> transform(acc) }

    private fun applyAftersTo(fixture: F) {
        afters.forEach { afterFn ->
            afterFn(fixture)
        }
    }

    override fun withChildren(children: List<RuntimeNode<F>>) = copy(children = children)

    // apply befores in order - if anything is thrown return it and the last successful value
    private fun applyBeforesTo(fixture: F): OpResult<F> {
        befores.forEach { beforeFn ->
            try {
                beforeFn(fixture)
            } catch (t: Throwable) {
                return OpResult(t, fixture)
            }
        }
        return OpResult(null, fixture)
    }
}
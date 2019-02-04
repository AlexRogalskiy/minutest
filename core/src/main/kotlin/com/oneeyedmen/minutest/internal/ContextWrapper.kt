package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.*
import com.oneeyedmen.minutest.experimental.TestAnnotation

internal data class ContextWrapper<PF, F>(
    override val name: String,
    override val annotations: List<TestAnnotation>,
    override val children: List<Node<F>>,
    val runner: (Testlet<F>, parentFixture: PF, TestDescriptor) -> F,
    val onClose: () -> Unit
) : Context<PF, F>() {

    constructor(
        delegate: Context<PF, F>,
        name: String = delegate.name,
        properties: List<TestAnnotation> = delegate.annotations,
        children: List<Node<F>> = delegate.children,
        runner: (Testlet<F>, parentFixture: PF, TestDescriptor) -> F = delegate::runTest,
        onClose: () -> Unit = delegate::close
        ) : this(name, properties, children, runner, onCloseFor(delegate, onClose))



    override fun runTest(testlet: Testlet<F>, parentFixture: PF, testDescriptor: TestDescriptor): F =
        runner(testlet, parentFixture, testDescriptor)

    override fun withTransformedChildren(transform: NodeTransform) = copy(children = transform.applyTo(children))


    override fun close() = onClose.invoke()
}

// We always want to call close on the delegate, but only once
private fun <PF, F> onCloseFor(delegate: Context<PF, F>, specified: () -> Unit): () -> Unit =
    if (specified == delegate::close) specified else {
        {
            delegate.close()
            specified()
        }
    }
package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.Context
import com.oneeyedmen.minutest.TestDescriptor
import com.oneeyedmen.minutest.TestTransform
import kotlin.reflect.KType

internal class ContextBuilder<PF, F>(
    private val name: String,
    private val type: KType,
    private var fixtureFactory: ((PF, TestDescriptor) -> F)?,
    private var explicitFixtureFactory: Boolean
) : Context<PF, F>(), NodeBuilder<PF> {

    private val children = mutableListOf<NodeBuilder<F>>()
    private val befores = mutableListOf<(F) -> Unit>()
    private val afters = mutableListOf<(F) -> Unit>()
    private val transforms = mutableListOf<TestTransform<F>>()

    override fun privateDeriveFixture(f: (parentFixture: PF, testDescriptor: TestDescriptor) -> F) {
        if (explicitFixtureFactory)
            throw IllegalStateException("Fixture already set in context \"$name\"")
        fixtureFactory = f
        explicitFixtureFactory = true
    }

    override fun before(operation: F.() -> Unit) {
        befores.add(operation)
    }

    override fun after(operation: F.() -> Unit) {
        afters.add(operation)
    }

    override fun test_(name: String, f: F.() -> F) {
        children.add(TestBuilder(name, f))
    }

    override fun context(name: String, builder: Context<F, F>.() -> Unit) =
        privateCreateSubContext(name, type, { this }, false, builder)

    override fun <G> privateCreateSubContext(
        name: String,
        type: KType,
        fixtureFactory: (F.(TestDescriptor) -> G)?,
        explicitFixtureFactory: Boolean,
        builder: Context<F, G>.() -> Unit
    ) {
        children.add(ContextBuilder(name,
            type,
            fixtureFactory,
            explicitFixtureFactory).apply(builder))
    }

    override fun addTransform(transform: TestTransform<F>) {
        transforms.add(transform)
    }

    override fun buildNode(parent: ParentContext<PF>): PreparedRuntimeContext<PF, F> {
        val fixtureFactory = resolvedFixtureFactory()
        return PreparedRuntimeContext(name,
            parent,
            emptyList(),
            befores,
            afters,
            transforms,
            fixtureFactory).let { context ->
            // nastiness to set up parent child in immutable nodes
            context.copy(children = this.children.map { child -> child.buildNode(context) })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvedFixtureFactory(): (PF, TestDescriptor) -> F {
        return when {
            fixtureFactory != null -> fixtureFactory
            thisContextDoesntNeedAFixture() -> { _, _ -> Unit as F }
            // this is safe provided there are only fixture not replaceFixture calls in sub-contexts,
            // as we cannot provide a fixture here to act as receiver. TODO - check somehow
            else -> error("Fixture has not been set in context \"$name\"")
        }!!
    }

    private fun thisContextDoesntNeedAFixture() =
        befores.isEmpty() && afters.isEmpty() && children.filterIsInstance<TestBuilder<F>>().isEmpty()
}
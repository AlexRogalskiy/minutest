package dev.minutest.internal

import dev.minutest.*
import dev.minutest.experimental.RootTransform
import dev.minutest.experimental.TestAnnotation
import dev.minutest.experimental.annotateWith

internal data class MinutestRootContextBuilder<F>(
    private val name: String,
    private val type: FixtureType,
    private val builder: TestContextBuilder<Unit, F>.() -> Unit,
    private val transform: (Node<Unit>) -> Node<Unit>,
    private val annotations: MutableList<TestAnnotation<Unit>> = mutableListOf()
) : RootContextBuilder<F> {


    override fun buildNode(): Node<Unit> {
        // we need to apply our annotations to the root, then run the transforms
        val rootBuilder = rootBuilder(name, type, builder).apply {
            annotateWith(this@MinutestRootContextBuilder.annotations)
        }
        val untransformed = rootBuilder.buildNode()
        val transformsInTree = untransformed.findRootTransforms()
        val myRootTransform = transform.asRootTransform()
        val deduplicatedTransforms = (transformsInTree + myRootTransform).toSet() // [1]
        val transform = deduplicatedTransforms.reduce { a, b -> a.then(b) }
        return transform.transformRoot(untransformed)
    }

    override fun annotateWith(annotation: TestAnnotation<in Unit>) {
        annotations.add(annotation as TestAnnotation<Unit>)
    }

    // 1 - using the transforms in the tree first keeps tests passing, largely I think because it allows FOCUS to
    // be applied before logging.
}

private fun ((Node<Unit>) -> Node<Unit>).asRootTransform() =
    object : RootTransform {
        override fun transformRoot(node: Node<Unit>): Node<Unit> =
            this@asRootTransform(node)
    }

private fun <F> rootBuilder(name: String, type: FixtureType, builder: TestContextBuilder<Unit, F>.() -> Unit) =
    MinutestContextBuilder<Unit, F>(name, type, rootFixtureFactoryHack()).apply(builder)

// TODO - this should probably be breadth-first
private fun Node<*>.findRootTransforms(): List<RootTransform> {
    val myTransforms: List<RootTransform> = annotations.filterIsInstance<RootTransform>()
    return when (this) {
        is Test<*> -> myTransforms
        is Context<*, *> -> myTransforms + this.children.flatMap { it.findRootTransforms() }
    }
}
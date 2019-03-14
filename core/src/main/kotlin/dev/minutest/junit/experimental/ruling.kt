package dev.minutest.junit.experimental

import dev.minutest.*
import dev.minutest.experimental.ContextWrapper
import dev.minutest.experimental.TestAnnotation
import dev.minutest.experimental.annotateWith
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.Description.createTestDescription
import org.junit.runners.model.Statement

/**
 * Apply a JUnit test rule in a fixture
 */
fun <PF, F, R: TestRule> TestContextBuilder<PF, F>.applyRule(ruleExtractor: F.() -> R) {
    annotateWith(
        TestRuleAnnotation(ruleExtractor)
    )
}

private class TestRuleAnnotation<PF, F, R: TestRule>(
    private val ruleExtractor: F.() -> R
) : TestAnnotation<PF> {

    override fun <F2 : PF> transformOfType(): NodeTransform<F2> = NodeTransform { node ->
        when (node) {
            is Context<F2, *> -> {
                @Suppress("UNCHECKED_CAST")
                ContextWrapper<PF, F>(node as Context<PF, F>, runner = magicRunnerFor(node))
            }
            else -> error("TestRuleAnnotation can only be applied to a context")
        }
    }

    private fun magicRunnerFor(context: Context<PF, F>) =
        fun(testlet: Testlet<F>, parentFixture: PF, testDescriptor: TestDescriptor) =
            context.runTest(magicTestlet(testlet), parentFixture, testDescriptor)

    private fun magicTestlet(wrapped: Testlet<F>) = fun(fixture: F, testDescriptor: TestDescriptor): F {

        var fixureValue: F? = null // 1 - will be supplied at [2]

        val wrappedTestAsStatement = object: Statement() {
            override fun evaluate() {
                fixureValue = wrapped(fixture, testDescriptor) // 2 - see [1]
            }
        }

        // this is Statement.apply
        ruleExtractor(fixture).apply(wrappedTestAsStatement, testDescriptor.toTestDescription()).evaluate()

        @Suppress("UNCHECKED_CAST")
        return fixureValue as F // F may be nullable, so this can be initialised and still null, so cast not !!
    }

    private fun TestDescriptor.toTestDescription(): Description = fullName().let {
        createTestDescription(it.first(), it.drop(1).joinToString("."))
    }
}

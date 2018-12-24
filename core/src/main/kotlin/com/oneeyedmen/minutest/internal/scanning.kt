package com.oneeyedmen.minutest.internal

import com.oneeyedmen.minutest.RuntimeContext
import com.oneeyedmen.minutest.RuntimeNode
import com.oneeyedmen.minutest.Test
import com.oneeyedmen.minutest.TestDescriptor
import io.github.classgraph.*
import kotlin.reflect.KFunction0
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

internal data class ScannedPackageContext(
    val packageName: String,
    private val contextFuns: List<KFunction0<TopLevelContextBuilder<Unit>>>,
    override val properties: Map<Any, Any> = emptyMap()
) : RuntimeContext<Unit, Unit>() {

    override val name: String get() = packageName

    override val children: List<RuntimeNode<Unit>> by lazy {
        contextFuns.map { f ->
            f().copy(name = f.name).buildNode()
        }
    }

    override fun runTest(test: Test<Unit>, parentFixture: Unit, testDescriptor: TestDescriptor) =
        RootExecutor.runTest(test, testDescriptor)

    override fun withChildren(children: List<RuntimeNode<Unit>>): RuntimeContext<Unit, Unit> =
        TODO("not implemented")
    
    override fun close() {}
}

internal fun scan(scannerConfig: ClassGraph.() -> Unit, classFilter: (ClassInfo) -> Boolean = {true}): List<ScannedPackageContext> {
    return ClassGraph()
        .enableClassInfo()
        .enableMethodInfo()
        .disableJarScanning()
        .disableNestedJarScanning()
        .apply(scannerConfig)
        .scan()
        .allClasses
        .filter { it.isStandardClass && !it.isAnonymousInnerClass && it.isPublic && !it.isSynthetic }
        .filter(classFilter)
        .flatMap { it.declaredMethodInfo }
        .filter { it.definesTopLevelContext() }
        .mapNotNull { it.toKotlinFunction() }
        // Check Kotlin visibility because a public static Java method might have internal visibility in Kotlin
        .filter { it.visibility == PUBLIC }
        .groupBy { it.javaMethod?.declaringClass?.`package`?.name ?: "<tests>" }
        .map { (packageName, functions) -> ScannedPackageContext(packageName, functions as List<KFunction0<TopLevelContextBuilder<Unit>>>) }
}

private fun MethodInfo.toKotlinFunction(): KFunction0<TopLevelContextBuilder<*>>? {
    @Suppress("UNCHECKED_CAST")
    return loadClassAndGetMethod().kotlinFunction as? KFunction0<TopLevelContextBuilder<*>>
}

private fun MethodInfo.definesTopLevelContext() =
    isStatic && isPublic && parameterInfo.isEmpty() && !isBridge
        && typeSignatureOrTypeDescriptor.resultType.name() == TopLevelContextBuilder::class.java.name

private fun TypeSignature.name() =
    (this as? ClassRefTypeSignature)?.baseClassName


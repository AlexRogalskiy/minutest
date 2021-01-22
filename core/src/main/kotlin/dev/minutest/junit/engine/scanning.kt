package dev.minutest.junit.engine

import dev.minutest.Node
import dev.minutest.RootContextBuilder
import dev.minutest.internal.AmalgamatedRootContext
import dev.minutest.internal.rootContextForClass
import dev.minutest.internal.time
import io.github.classgraph.*
import kotlin.reflect.KFunction0
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun scanForRootNodes(
    scannerConfig: ClassGraph.() -> Unit,
    classFilter: (ClassInfo) -> Boolean = { true }
): List<Node<Unit>> =
    time("ClassGraph scanning ") {
        classGraphWith(scannerConfig)
            .scan()
            .use { scanResult ->
                val methodInfos: Sequence<MethodInfo> = scanResult
                    .allClasses
                    .asSequence()
                    .filter {
                        classFilter(it) &&
                            it.isStandardClass && !it.isAnonymousInnerClass && it.isPublic && !it.isSynthetic &&
                            it.hasMethodAnnotation("org.junit.platform.commons.annotation.Testable")
                    }
                    .flatMap { it.declaredMethodInfo.asSequence() }

                val (methods, functions) = methodInfos
                    .filter { it.definesARootContext() }
                    .partition { !it.isStatic }

                val methodContexts: List<Node<Unit>> = methods
                    .mapNotNull { it.toKotlinFunction()?.javaMethod?.declaringClass }
                    .toSet()
                    .map { rootContextForClass(it.kotlin, flattenSingleNode = true) }

                val topLevelContexts = functions
                    .mapNotNull { it.toKotlinFunction() }
                    // Check Kotlin visibility because a public static Java method might have internal visibility in Kotlin
                    .filter { it.visibility == PUBLIC }
                    .groupBy { it.javaMethod?.declaringClass?.`package`?.name ?: "<tests>" }
                    .map { (packageName, functions: List<RootContextFun>) ->
                        AmalgamatedRootContext(packageName, functions.renamed().map { it.buildNode() })
                    }
                (methodContexts + topLevelContexts)
            }
    }



private fun classGraphWith(scannerConfig: ClassGraph.() -> Unit) =
    ClassGraph()
        .enableClassInfo()
        .enableMethodInfo()
        .enableAnnotationInfo()
        .disableJarScanning()
        .disableNestedJarScanning()
        .apply(scannerConfig)

@Suppress("UNCHECKED_CAST") // reflection
private fun MethodInfo.toKotlinFunction(): RootContextFun? =
    loadClassAndGetMethod().kotlinFunction as? RootContextFun

private fun MethodInfo.definesARootContext() =
    isPublic && parameterInfo.isEmpty() && !isBridge &&
        typeSignatureOrTypeDescriptor.resultType.name() == RootContextBuilder::class.java.name
        && hasAnnotation("org.junit.platform.commons.annotation.Testable")

private fun TypeSignature.name() = (this as? ClassRefTypeSignature)?.baseClassName

private fun Iterable<RootContextFun>.renamed(): List<RootContextBuilder> =
    this.map { f: RootContextFun ->
        f().withNameUnlessSpecified(f.name)
    }

private typealias RootContextFun = KFunction0<RootContextBuilder>
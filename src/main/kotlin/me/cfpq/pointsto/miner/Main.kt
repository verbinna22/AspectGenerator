package me.cfpq.pointsto.miner

import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.JcMethod
import org.jacodb.impl.features.classpaths.UnknownClassMethodsAndFields
import org.jacodb.impl.features.classpaths.UnknownClasses
import org.jacodb.impl.jacodb
import org.jacodb.api.jvm.cfg.JcCallInst

import java.io.File
import kotlin.streams.asStream

val mainDirectory = "/mnt/data/MyOwnFolder/learning/p_algo/logging-log4j2"

suspend fun main(args: Array<String>) {
//    showFunId = false to old version
//    var joinedPointCut: String = ""
    var allMethods = ""
    useJacoDb { cp ->
        val methods = cp.safeAllClasses()
            .flatMap {
                try {
                    it.declaredMethods.asSequence()
                } catch (_: Exception) {
                    emptySequence()
                }.asStream()
            }
            .map { callMethod -> callMethod.builderNameStandart }
            .distinct()
            .filter { str -> str.startsWith("java.lang.") || str.startsWith("java.util.") }
            .toList()
        allMethods = methods
            .joinToString("\n")
    }
    File("./stdlib_methods.txt").writer().use { writer ->
        writer.write(allMethods)
    }
}

val JcMethod.builderNameStandart
    get() = "${this.enclosingClass.name}#${this.name}(${this.parameters.joinToString(",") { it.type.typeName }})"

suspend fun useJacoDb(block: (JcClasspath) -> Unit) = jacodb { keepLocalVariableNames() }.use { db ->
    db.classpath(getRuntimeClasspath(), listOf(UnknownClassMethodsAndFields, UnknownClasses,
    )).use(block)
}

private fun getRuntimeClasspath(): List<File> {
    val classpath = System.getProperty("java.class.path")
    val classpathFiles = classpath.split(File.pathSeparator)
        .filter { it.isNotEmpty() }
        .map { File(it) }
    return classpathFiles
}

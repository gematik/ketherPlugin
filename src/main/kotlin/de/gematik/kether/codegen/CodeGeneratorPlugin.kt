package de.gematik.kether.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Created by rk on 26.09.2022.
 * gematik.de
 */

class CodeGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("codegen",CodeGeneratorPluginExtension::class.java,)
        project.task("convertABI") {
            it.doLast {
                val inputDir = project.file("src")
                val abiFiles = collectAbiFiles(inputDir)
//                val abiFiles = listOf(project.file("src/main/kotlin/de/gematik/kether/contracts/Storage.abi"))
                abiFiles.forEach {
                    println("Proessing ${it.name} ...")
                    val byteCodeFile = File(it.absolutePath.dropLast(4) + ".bytecode")
                    val code = CodeGenerator(
                        extension.packageName.orNull ?: "de.gematik.kether.codegen",
                        abiFile = it,
                        byteCodeFile = if (byteCodeFile.exists()) byteCodeFile else null
                    ).generateCode()
                    val outfile = File(it.absolutePath.dropLast(4) + ".kt")
                    outfile.writeText(code)
                }
            }
        }
    }

    private fun collectAbiFiles(file: File): List<File> {
        val list = mutableListOf<File>()
        list.addAbiFiles(file)
        return list
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun MutableList<File>.addAbiFiles(file: File) {
        file.listFiles()?.forEach {
            when {
                it.isDirectory -> addAbiFiles(it)
                else -> if (it.name.lowercase().endsWith(".abi")) add(it)
            }
        }
    }
}


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
        val extension = project.extensions.create("codegen", CodeGeneratorPluginExtension::class.java)
        project.task("convertABI") {
            it.doLast {
                val inputDir = project.file("src")
                val abiFiles = collectAbiFiles(inputDir)
                abiFiles.forEach {
                    println("Proessing ${it.name} ...")
                    var byteCodeFile: File? = File(it.absolutePath.dropLast(4) + ".bytecode")
                    if (!byteCodeFile!!.exists()) {
                        byteCodeFile = File(it.absolutePath.dropLast(4) + ".bin")
                    }
                    if (!byteCodeFile.exists()) {
                        byteCodeFile = null
                    }
                    val code = CodeGenerator(
                        extension.packageName.orNull ?: "de.gematik.kether.codegen",
                        abiFile = it,
                        byteCodeFile = byteCodeFile
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


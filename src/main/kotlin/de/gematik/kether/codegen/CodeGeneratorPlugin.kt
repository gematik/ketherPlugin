package de.gematik.kether.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import de.gematik.kether.solckt.SolidityFile
import de.gematik.kether.solckt.SolcArguments
import org.gradle.api.Task

/**
 * Created by rk on 26.09.2022.
 * gematik.de
 */

class CodeGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("codegen", CodeGeneratorPluginExtension::class.java)
        project.task("compileSolidity") {
            val packageName = extension.packageName.orNull ?: "de.gematik.scuma.contracts"
            println(packageName)
            val inputDir = project.file("src")
            val solFiles = collectFiles(inputDir, ".sol")
            solFiles.forEach {
                println("Compiling ${it.name} ...")
                val solidityFile = SolidityFile(it.absoluteFile.toString())
                val compilerInstance = solidityFile.getCompilerInstance()
                val result = compilerInstance.execute(
                    SolcArguments.OUTPUT_DIR.param { it.parent.toString()},
                    SolcArguments.BIN,
                    SolcArguments.ABI,
                    SolcArguments.OVERWRITE,
                )
            }
            convertABI(project,it,packageName)
        }
        project.task("convertABI") {
            val packageName = extension.packageName.orNull ?: "de.gematik.kether.codegen"
            convertABI(project,it,packageName)
        }
    }

    private fun convertABI(project: Project, task: Task, packageName: String){
        task.doLast {
            val inputDir = project.file("src")
            val abiFiles = collectFiles(inputDir, ".abi")
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
                    packageName = packageName,
                    abiFile = it,
                    byteCodeFile = byteCodeFile
                ).generateCode()
                val outfile = File(it.absolutePath.dropLast(4) + ".kt")
                outfile.writeText(code)
            }
        }
    }

    private fun collectFiles(file: File, suffix: String): List<File> {
        val list = mutableListOf<File>()
        list.addFiles(file, suffix)
        return list
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun MutableList<File>.addFiles(file: File, suffix: String) {
        file.listFiles()?.forEach {
            when {
                it.isDirectory -> addFiles(it, suffix)
                else -> if (it.name.lowercase().endsWith(suffix)) add(it)
            }
        }
    }
}


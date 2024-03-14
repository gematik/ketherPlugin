/*
 * Copyright 2022-2024, gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.gematik.kether.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import de.gematik.kether.solckt.SolidityFile
import de.gematik.kether.solckt.SolcArguments
import org.gradle.api.Task

/**
 * Created by rk on 26.09.2022.
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


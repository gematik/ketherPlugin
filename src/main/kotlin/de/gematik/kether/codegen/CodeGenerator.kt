package de.gematik.kether.codegen

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

/**
 * Created by rk on 20.09.2022.
 * gematik.de
 */
@OptIn(ExperimentalStdlibApi::class)
class CodeGenerator(
    packageName: String,
    contractName: String,
    private val abi: Array<Type>,
    private val byteCode: String? = null
) {
    constructor(packageName: String, abiFile: File, byteCodeFile: File?) : this(
        packageName,
        abiFile.name.substring(0, abiFile.name.indexOfLast { it == '.' }),
        abi = json.decodeFromString(abiFile.readText(Charsets.UTF_8)),
        byteCode = byteCodeFile?.let {
            when (it.extension.lowercase()) {
                "bytecode" -> Json.parseToJsonElement(it.readText(Charsets.UTF_8)).jsonObject.get("object")?.jsonPrimitive?.content
                "bin" -> it.readText(Charsets.UTF_8)
                else -> error("wrong extension of binary file")
            }
        })

    private val template = """package $packageName
        import de.gematik.kether.abi.DataDecoder
        import de.gematik.kether.abi.DataEncoder
        import de.gematik.kether.abi.isTypeDynamic
        import de.gematik.kether.abi.types.*
        import de.gematik.kether.contracts.Contract
        import de.gematik.kether.contracts.Event
        import de.gematik.kether.eth.Eth
        import de.gematik.kether.eth.types.*
        import de.gematik.kether.extensions.hexToByteArray
        import de.gematik.kether.extensions.keccak
        import kotlinx.serialization.ExperimentalSerializationApi
        import java.math.BigInteger

        @OptIn(ExperimentalSerializationApi::class)
        class $contractName(
            eth: Eth,
            baseTransaction: Transaction = Transaction()
        ) : Contract(eth, baseTransaction) {

            companion object {

                // deployment
                ${generateDeployment()}

                // 4 byte selectors (functions) and topics (events)
                ${generateSelectors()}
            }

            // tuples
            ${generateTuples()}

            // events
            ${generateEvents()}
            
            // functions
            ${generateFunctions()}

        }
    """.replace(Regex("""^\s+""", RegexOption.MULTILINE), "")

    private enum class StateMutability {
        view,
        pure,
        nonpayable,
        payable
    }

    fun generateCode(): String {
        return template
    }

    private fun generateDeployment(): String {
        byteCode ?: return "// deployment data (bytecode) not available"
        val stringBuilder = StringBuilder()
        val stringBuilderParams = StringBuilder()
        val constructors = abi.filter {
            it.type == "constructor"
        }
        stringBuilder.append("val byteCode = \"0x$byteCode\".hexToByteArray()\n")
        if (constructors.isEmpty()) {
            stringBuilder.append("fun deploy(eth:Eth, from: Address) = deploy(eth, from, Data(byteCode))")
        }
        constructors.forEach {
            stringBuilder.append("fun deploy(eth:Eth, from: Address,")
            stringBuilderParams.append("val params = Data(\nbyteCode + DataEncoder()\n")
            it.inputs?.forEach {
                stringBuilder.append("${it.name}: Abi${it.type.replaceFirstChar(Char::titlecase)},")
                stringBuilderParams.append(".encode(${it.name})")
            }
            stringBuilderParams.append(".build().toByteArray()\n)\n")
            if (stringBuilder.last() == ',') { stringBuilder.deleteAt(stringBuilder.length - 1) }
            stringBuilder.append("): TransactionReceipt {\n")
            stringBuilder.append(stringBuilderParams)
            stringBuilder.append("return deploy(eth, from, params)\n}\n")
        }
        return stringBuilder.toString()
    }

    private fun generateSelectors(): String {
        val stringBuilder = StringBuilder()
        abi.filter {
            it.type == "event"
        }.forEach {
            it.name?.let { name ->
                stringBuilder.append("val event${name.replaceFirstChar(Char::titlecase)} = Data32(\"$name(")
                it.inputs?.forEach {
                    stringBuilder.append(it.signatureOfType() + ",")
                }
                if (stringBuilder.last() == ',') {
                    stringBuilder.deleteAt(stringBuilder.length - 1)
                }
                stringBuilder.append(
                    ")\".keccak())\n"
                )
            }
        }
        abi.filter {
            it.type == "function"
        }.forEach {
            it.name!!.let { name ->
                stringBuilder.append("val function${name.replaceFirstChar(Char::titlecase)} = \"$name(")
                it.inputs?.forEach {
                    stringBuilder.append(it.signatureOfType() + ",")
                }
                if (stringBuilder.last() == ',') {
                    stringBuilder.deleteAt(stringBuilder.length - 1)
                }
                stringBuilder.append(
                    ")\".keccak().copyOfRange(0, 4)\n"
                )
            }
        }
        return stringBuilder.toString()
    }

    private fun generateTuples(): String {
        val stringBuilder = StringBuilder()
        val tuples = mutableListOf<Component>()
        abi.forEach { it.inputs?.let { inputs -> tuples.addAll(inputs.getAllTuples()) } }
        abi.forEach { it.outputs?.let { outputs -> tuples.addAll(outputs.getAllTuples()) } }
        tuples.forEach {
            stringBuilder.append("data class ${it.typeNameStrippedDimension}${it.components?.params() ?: ""} : AbiTuple {\n")
            // decoder
            stringBuilder.append("constructor(dataDecoder: DataDecoder) : this(")
            it.components?.forEach {
                stringBuilder.append("dataDecoder.next(${it.typeNameStrippedDimension}::class),")
            }
            if (stringBuilder.last() == ',') stringBuilder.deleteAt(stringBuilder.length - 1)
            stringBuilder.append(")")
            // encoder
            stringBuilder.append("override fun encode(): DataEncoder {\n")
            stringBuilder.append("return DataEncoder()\n")
            it.components?.forEach {
                stringBuilder.append(".encode(${it.name})\n")
            }
            stringBuilder.append("}\n")
            // companion object
            stringBuilder.append("companion object : Dynamic {\n")
            stringBuilder.append("override fun isDynamic() = ")
            var isDynamic = false
            it.components?.forEach {
                if (it.dimensions.contains("-1")) isDynamic = true
            }
            if (isDynamic) {
                stringBuilder.append("true")
            } else {
                it.components?.forEach {
                    stringBuilder.append("isTypeDynamic(${it.typeNameStrippedDimension}::class)||")
                }
                if (stringBuilder.last() == '|') stringBuilder.delete(
                    stringBuilder.length - 2,
                    stringBuilder.length
                )
            }
            stringBuilder.append("}\n}\n")
        }
        return stringBuilder.toString()
    }

    private fun generateEvents(): String {
        val stringBuilder = StringBuilder()
        val stringBuilderEventDecoders = StringBuilder()
        abi.filter {
            it.type == "event"
        }.forEach {
            if (it.name != null) {
                val eventClassName = "Event${it.name.replaceFirstChar(Char::titlecase)}"
                stringBuilderEventDecoders.append("$eventClassName::decoder,")
                stringBuilder.append("data class $eventClassName(")
                stringBuilder.append("val eventSelector: AbiBytes32,")
                val stringBuilderTopics = StringBuilder()
                stringBuilderTopics.append("eventSelector,")
                val stringBuilderValues = StringBuilder()
                val stringBuilderArguments = StringBuilder()
                stringBuilderArguments.append("eventSelector=log.topics!!.get(0),")
                var index = 1
                it.inputs?.forEach {
                    if (it.indexed == true) {
                        stringBuilder.append("val ${it.name}: AbiBytes32,")
                        stringBuilderTopics.append("${it.name},")
                        stringBuilderArguments.append("${it.name} = log.topics!!.get(${index++}),")
                    } else {
                        val abiTypeName = "Abi${it.type.replaceFirstChar(Char::titlecase)}"
                        stringBuilder.append("val ${it.name}: $abiTypeName,")
                        stringBuilderValues.append("val ${it.name} = decoder.next($abiTypeName::class)\n")
                        stringBuilderArguments.append("${it.name} = ${it.name},")
                    }
                }
                if (stringBuilder.last() == ',') stringBuilder.deleteAt(stringBuilder.length - 1)
                if (stringBuilderTopics.last() == ',') stringBuilderTopics.deleteAt(stringBuilderTopics.length - 1)
                stringBuilder.append(") : Event(topics = listOf(${stringBuilderTopics})) {\n")
                stringBuilder.append("companion object {\n")
                stringBuilder.append("fun decoder(log: Log): Event? {\n")
                stringBuilder.append("return checkEvent(log, event${it.name})?.let {\n")
                stringBuilder.append("val decoder = DataDecoder(log.data!!)\n")
                stringBuilder.append(stringBuilderValues)
                stringBuilder.append("$eventClassName(\n")
                if (stringBuilderArguments.last() == ',') stringBuilderArguments.deleteAt(stringBuilderArguments.length - 1)
                stringBuilder.append(stringBuilderArguments)
                stringBuilder.append(")\n}\n}\n}\n}\n")
            }
        }
        if (!stringBuilderEventDecoders.isEmpty() && stringBuilderEventDecoders.last() == ',') stringBuilderEventDecoders.deleteAt(
            stringBuilderEventDecoders.length - 1
        )
        stringBuilder.append("override val listOfEventDecoders: List<(Log) -> Event?> = listOf($stringBuilderEventDecoders)")
        return stringBuilder.toString()
    }

    private fun generateFunctions(): String {
        val stringBuilder = StringBuilder()
        abi.filter {
            it.type == "function"
        }.forEach {
            if (it.name != null) {
                val stateMutability = it.stateMutability?.let { StateMutability.valueOf(it) }
                var resultClassName: String? = null
                if (it.outputs != null && it.outputs.size > 1 && stateMutability != StateMutability.payable && stateMutability != StateMutability.nonpayable
                ) {
                    resultClassName = "Results${it.name.replaceFirstChar(Char::titlecase)}"
                    stringBuilder.append("data class $resultClassName(\n")
                    it.outputs.forEach {
                        val name = it.name?.let {
                            if (it.isEmpty()) "value" else it
                        } ?: ""
                        stringBuilder.append("val $name: ${it.typeName}\n")
                    }
                    stringBuilder.append(")\n")
                }
                val stringBuilderParams = StringBuilder()
                if (stateMutability == StateMutability.payable || stateMutability == StateMutability.nonpayable) {
                    stringBuilder.append("suspend fun ${it.name}(")
                    stringBuilderParams.append(
                        "val params = DataEncoder()\n.encode(Data4(function${
                            it.name.replaceFirstChar(
                                Char::titlecase
                            )
                        }))"
                    )
                    it.inputs?.forEach {
                        stringBuilder.append("${it.name}: ${it.typeName},")
                        stringBuilderParams.append("\n.encode(${it.name}")
                        stringBuilderParams.append(if (it.dimensions.isNotEmpty()) ", ${it.dimensions})" else ")")
                    }
                    stringBuilderParams.append(".build()\n")
                    if (stringBuilder.last() == ',') stringBuilder.deleteAt(stringBuilder.length - 1)
                    stringBuilder.append("): TransactionReceipt {\n")
                    stringBuilder.append(stringBuilderParams.toString())
                    stringBuilder.append("return transact(params)\n}\n")
                } else {
                    stringBuilder.append("fun ${it.name}(")
                    stringBuilderParams.append(
                        "val params = DataEncoder()\n.encode(Data4(function${
                            it.name.replaceFirstChar(
                                Char::titlecase
                            )
                        }))"
                    )
                    it.inputs?.forEach {
                        stringBuilder.append("${it.name}: ${it.typeName},")
                        stringBuilderParams.append("\n.encode(${it.name}")
                        stringBuilderParams.append(if (it.dimensions.isNotEmpty()) ", ${it.dimensions})" else ")")
                    }
                    stringBuilderParams.append(".build()\n")
                    if (stringBuilder.last() == ',') stringBuilder.deleteAt(stringBuilder.length - 1)
                    if (it.outputs != null && it.outputs.size == 1) {
                        resultClassName = it.outputs[0].typeName
                    }
                    stringBuilder.append(if (resultClassName != null) "): $resultClassName {\n" else "){\n")
                    stringBuilder.append(stringBuilderParams.toString())
                    stringBuilder.append("val decoder = DataDecoder(call(params))\n")
                    if (it.outputs != null && it.outputs.size == 1) {
                        stringBuilder.append("return decoder.next(${it.outputs[0].typeNameStrippedDimension}::class")
                        stringBuilder.append(if (it.outputs[0].dimensions.isNotEmpty()) ", ${it.outputs[0].dimensions})" else ")")
                    } else {
                        stringBuilder.append("return $resultClassName(\ndecoder")
                        it.outputs?.forEach {
                            stringBuilder.append("\n.next()")
                        }
                        stringBuilder.append(")\n")
                    }
                    stringBuilder.append("}\n")
                }
            }
        }
        return stringBuilder.toString()
    }
}
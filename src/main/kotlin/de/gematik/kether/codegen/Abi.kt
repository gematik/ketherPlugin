package de.gematik.`kether-plugin`.codegen

import kotlinx.serialization.Serializable
import org.gradle.configurationcache.extensions.capitalized

/**
 * Created by rk on 14.10.2022.
 * gematik.de
 */

@Serializable
data class Type(
    val type: String, // "function", "constructor", "receive", "fallback", "event", "error"
    val name: String? = null,
    val inputs: List<Component>? = null,
    val outputs: List<Component>? = null,
    val stateMutability: String? = null,
    val anonymous: Boolean? = null
)

@Serializable
data class Component(
    val name: String? = null,
    val type: String, // uint256, string, bytes32, tuple, tuple[] ...
    val internalType: String? = null,
    val components: List<Component>? = null,
    val indexed: Boolean? = null
) {
    val typeNameStrippedDimension: String by lazy {
        if (type.startsWith("tuple")) {
            internalType!!.substringAfterLast('.').substringBefore('[')
        } else {
            "Abi${type.substringBefore('[').capitalized()}"
        }
    }

    val typeName: String by lazy {
        var s = typeNameStrippedDimension
        repeat(type.count { it == '[' }){ s = "List<$s>" }
        s
    }

    val dimensions: String by lazy {
        val stringBuilder = StringBuilder()
        val split = type.split('[')
        for (i in 1 until split.size ) {
            val dim = split[i].substringBefore(']')
                stringBuilder.append(if(dim.isEmpty()) "-1" else dim)
        }
        stringBuilder.toString()
    }

    fun signatureOfType(): String {
        return if (type.startsWith("tuple")) {
            val stringBuilder = StringBuilder()
            components?.forEach {
                stringBuilder.append(it.signatureOfType() + ",")
            }
            if (stringBuilder.last() == ',') {
                stringBuilder.deleteAt(stringBuilder.length - 1)
            }

            type.replace("tuple", "(${stringBuilder})")
        } else {
            type
        }
    }

}

fun List<Component>.getAllTuples(): List<Component> {
    val list = mutableListOf<Component>()
    forEach {
        if (it.type.startsWith("tuple")) {
            list.add(it)
            list.addAll(it.components!!.getAllTuples())
        }
    }
    return list
}

fun List<Component>.params(): String {
    val stringBuilder = StringBuilder("(")
    forEach {
        stringBuilder.append("val ${it.name}: ${it.typeNameStrippedDimension},")
    }
    if (stringBuilder.last() == ',') {
        stringBuilder.deleteAt(stringBuilder.length - 1)
    }
    stringBuilder.append(
        ")"
    )
    return stringBuilder.toString()
}

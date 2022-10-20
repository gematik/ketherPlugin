package de.gematik.kether.codegen

import kotlinx.serialization.Serializable

/**
 * Created by rk on 14.10.2022.
 * gematik.de
 */

@Serializable
data class Type(
    val type: String, // "function", "constructor", "receive", "fallback", "event", "error"
    val name: String? = null,
    val inputs: Array<Component>? = null,
    val outputs: Array<Component>? = null,
    val stateMutability: String? = null,
    val anonymous: Boolean? = null
)

@Serializable
data class Component(
    val name: String? = null,
    val type: String, // uint256, string, bytes32, tuple, tuple[] ...
    val internalType: String? = null,
    val components: Array<Component>? = null,
    val indexed: Boolean? = null
)
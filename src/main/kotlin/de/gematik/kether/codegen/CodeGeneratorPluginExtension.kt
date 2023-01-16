package de.gematik.kether.codegen

import org.gradle.api.provider.Property

/**
 * Created by rk on 26.09.2022.
 * gematik.de
 */
interface CodeGeneratorPluginExtension {
    val packageName: Property<String>
}
package de.gematik.kether.codegen

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/**
 * Created by rk on 20.09.2022.
 * gematik.de
 */
class CodeGenTest {
    @Test
    fun scumaContractTest() {
        val code = CodeGenerator(
            "de.gematik.kether.codegen",
            File("src/test/kotlin/de/gematik/kether/codegen/ScumaContract.abi"),
            File("src/test/kotlin/de/gematik/kether/codegen/ScumaContract.bin")
        ).generateCode()
        File("src/test/kotlin/de/gematik/kether/codegen/ScumaContract.ref").writeText(code)
        assert(code.startsWith("package de.gematik.kether.codegen"))
    }

    @Test
    fun storageContractTest() {
        val code = CodeGenerator(
            "de.gematik.kether.codegen",
            File("src/test/kotlin/de/gematik/kether/codegen/Storage.abi"),
            null
        ).generateCode()
        File("src/test/kotlin/de/gematik/kether/codegen/Storage.ref").writeText(code)
        assert(code.startsWith("package de.gematik.kether.codegen"))
    }

}
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

import org.junit.jupiter.api.Test
import java.io.File

/**
 * Created by rk on 20.09.2022.
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
            File("src/test/kotlin/de/gematik/kether/codegen/Storage.bin")
        ).generateCode()
        File("src/test/kotlin/de/gematik/kether/codegen/Storage.ref").writeText(code)
        assert(code.startsWith("package de.gematik.kether.codegen"))
    }

}
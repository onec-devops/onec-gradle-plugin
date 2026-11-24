package io.github.onecdevops

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.charset.Charset
import kotlin.test.assertEquals

class ProcessOutputTest {

    private val windows1251 = Charset.forName("windows-1251")
    private val text = "Ошибка"

    @ParameterizedTest
    @ValueSource(strings = ["UTF-8", "windows-1251"])
    fun `decodes output in UTF-8 or the fallback encoding`(encoding: String) {
        val decoded = decodeProcessOutput(text.toByteArray(Charset.forName(encoding)), fallback = windows1251)

        assertEquals(text, decoded)
    }
}

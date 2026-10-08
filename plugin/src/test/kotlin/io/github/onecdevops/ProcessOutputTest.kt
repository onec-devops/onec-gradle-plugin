package io.github.onecdevops

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessOutputTest {

    private val windows1251 = Charset.forName("windows-1251")
    private val text = "Ошибка"

    @Test fun `decodes UTF-8 output as UTF-8`() {
        val decoded = decodeProcessOutput(text.toByteArray(Charsets.UTF_8), fallback = windows1251)

        assertEquals(text, decoded)
    }

    @Test fun `decodes output that is not UTF-8 in the fallback encoding`() {
        val decoded = decodeProcessOutput(text.toByteArray(windows1251), fallback = windows1251)

        assertEquals(text, decoded)
    }
}

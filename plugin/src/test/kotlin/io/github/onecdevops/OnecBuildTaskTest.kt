package io.github.onecdevops

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class OnecBuildTaskTest {

    @Test
    fun `lists the external objects of a 1C EDT project that holds several`() {
        val failure = edtExternalObjectsNotOne(
            File("src"),
            listOf("ExternalDataProcessors/DataProcessor", "ExternalReports/Report")
        )

        assertEquals(
            "The 1C:EDT project 'src' must hold one external data processor or report to build, it holds 2:\n" +
                "* ExternalDataProcessors/DataProcessor\n" +
                "* ExternalReports/Report\n" +
                "Put each external data processor or report in a 1C:EDT project of its own.",
            failure.message
        )
    }

    @Test
    fun `reports a 1C EDT project that holds no external objects`() {
        val failure = edtExternalObjectsNotOne(File("src"), emptyList())

        assertEquals(
            "The 1C:EDT project 'src' must hold one external data processor or report to build, it holds 0.\n" +
                "Put each external data processor or report in a 1C:EDT project of its own.",
            failure.message
        )
    }
}

package com.waheed.artificerx.core.agent

import com.google.common.truth.Truth.assertThat
import com.waheed.artificerx.core.network.FunctionCallDto
import com.waheed.artificerx.core.network.ToolCallDto
import org.junit.Test

class ArtifactToolParserTest {
    @Test fun create_zip_is_parsed() {
        val call = ToolCallDto("1", "function", FunctionCallDto("create_zip", "{\"file_name\":\"demo.zip\",\"files_json\":\"[]\"}"))
        assertThat(ToolCallParser.parse(call)).isInstanceOf(ParsedToolCall.CreateZip::class.java)
    }

    @Test fun generate_image_is_parsed() {
        val call = ToolCallDto("2", "function", FunctionCallDto("generate_image", "{\"prompt\":\"anime city\"}"))
        val parsed = ToolCallParser.parse(call) as ParsedToolCall.GenerateImage
        assertThat(parsed.prompt).isEqualTo("anime city")
    }
}

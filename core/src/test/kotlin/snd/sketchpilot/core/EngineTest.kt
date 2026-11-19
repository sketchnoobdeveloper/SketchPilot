package snd.sketchpilot.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import snd.sketchpilot.core.agent.AgentEvent
import snd.sketchpilot.core.agent.AgentLoop
import snd.sketchpilot.core.agent.AgentUi
import snd.sketchpilot.core.agent.ContextEngine
import snd.sketchpilot.core.agent.ToolRegistry
import snd.sketchpilot.core.build.BuildDiagnostics
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.project.ProjectEditor
import snd.sketchpilot.core.provider.LlmProvider
import snd.sketchpilot.core.provider.ProviderCallback
import snd.sketchpilot.core.provider.ProviderRegistry
import snd.sketchpilot.core.provider.ProviderRequest
import snd.sketchpilot.core.provider.SseParser
import snd.sketchpilot.core.tools.registerCoreTools
import snd.sketchpilot.core.mcp.McpDispatcher
import snd.sketchpilot.core.mcp.McpProtocol
import java.io.File

class EngineTest {
    @Test fun sseFraming() {
        val parser = SseParser()
        assertEquals(null, parser.feed("event: message"))
        assertEquals(null, parser.feed("data: {\"a\":1}"))
        val event = parser.feed("")
        assertEquals("message", event?.event)
        assertEquals("{\"a\":1}", event?.data)
        val doneParser = SseParser()
        doneParser.feed("data: [DONE]")
        assertTrue(doneParser.feed("")?.isDone == true)
    }

    @Test fun diagnosticsParsing() {
        val result = BuildDiagnostics.parse(listOf("aapt2: error: resource not found", "error: cannot find symbol"))
        assertEquals(2, result.length())
        assertEquals("resources", result.getJSONObject(0).getString("stage"))
        assertEquals("java", result.getJSONObject(1).getString("stage"))
    }

    @Test fun toolsAndAgentLoop() {
        val root = File("/tmp/sketchpilot-engine-test-${System.currentTimeMillis()}")
        val paths = Paths(root)
        paths.data("1").mkdirs()
        paths.metadata("1").parentFile.mkdirs()
        paths.metadata("1").writeBytes(SketchwareCrypto().encrypt("{\"sc_id\":\"1\"}"))
        paths.dataFile("1", "view").writeBytes(SketchwareCrypto().encrypt("@main.xml\n{\"id\":\"a\",\"type\":4}\n"))
        paths.dataFile("1", "logic").writeBytes(SketchwareCrypto().encrypt("@MainActivity.java_onCreate_initializeLogic\n"))
        val repo = OfflineRepository(paths)
        val editor = ProjectEditor(paths, repo)
        val registry = ToolRegistry()
        registerCoreTools(registry, repo, editor)
        assertTrue(registry.schemas().length() >= 6)

        val events = mutableListOf<String>()
        val providers = ProviderRegistry()
        var invocations = 0
        providers.register(object : LlmProvider {
            override val name = "mock"
            override fun stream(request: ProviderRequest, callback: ProviderCallback) {
                invocations++
                callback.onToken("hello")
                if (invocations == 1) {
                    callback.onToolCall(JSONObject().put("function", JSONObject()
                        .put("name", "project.list").put("arguments", "{}")))
                }
                callback.onDone()
            }
            override fun testConnection(apiKey: String, model: String, baseUrl: String) = JSONObject().put("ok", true)
        })
        val loop = AgentLoop(providers, registry, ContextEngine(), object : AgentUi {
            override fun onEvent(event: AgentEvent) { events.add(event.type) }
        })
        loop.providerName = "mock"
        loop.maxSteps = 2
        val result = loop.run("list projects")
        assertTrue(result.getBoolean("ok"))
        assertTrue(events.contains("tool_call"))
        assertTrue(events.contains("tool_result"))
        root.deleteRecursively()
    }

    @Test fun mcpProtocol() {
        assertTrue(McpProtocol.LATEST == "2026-07-28")
        assertTrue(McpDispatcher.constantTimeEquals("Bearer abc", "Bearer abc"))
        assertTrue(!McpDispatcher.constantTimeEquals("Bearer abc", "Bearer abd"))
        assertTrue(McpDispatcher.isLocalOrigin("http://127.0.0.1:8080"))
        assertTrue(!McpDispatcher.isLocalOrigin("https://evil.example"))
    }
}
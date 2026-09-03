package com.waheed.artificerx.core.routing

import com.waheed.artificerx.core.agent.AgentIntentRouter
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentIntentRouterTest {
    @Test
    fun detectsResearchIntent() {
        assertEquals(AgentIntentRouter.Kind.RESEARCH, AgentIntentRouter.route("research latest web sources and compare them").kind)
    }
}

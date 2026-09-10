package com.waheed.artificerx.core.ai.apex.agent

enum class SpecialistRole { SUPERVISOR, ARTIST, VISION, COLOR, RESEARCH, LAYOUT, ANIMATION, CRITIC, ARCHIVIST }

data class SpecialistProposal(val role: SpecialistRole, val summary: String, val confidence: Float, val dependencies: Set<SpecialistRole> = emptySet())

data class SupervisorDecision(val accepted: List<SpecialistProposal>, val rejected: List<SpecialistProposal>, val reason: String)

class MultiAgentSupervisor {
    fun decide(proposals: List<SpecialistProposal>): SupervisorDecision {
        val accepted = proposals
            .filter { it.confidence >= 0.6f }
            .sortedByDescending { it.confidence }
            .distinctBy { it.role }
        val acceptedRoles = accepted.map { it.role }.toSet()
        val resolved = accepted.filter { it.dependencies.all(acceptedRoles::contains) }
        val rejected = proposals.filterNot { it in resolved }
        return SupervisorDecision(resolved, rejected, "Deterministic confidence + dependency arbitration")
    }
}

package com.waheed.artificerx.research.web

data class ResearchPass(val query:String,val round:Int,val sources:Int,val contradictions:Int)
class DeepResearchCoordinator {
    fun shouldContinue(pass:ResearchPass,minimumSources:Int=4):Boolean = pass.sources < minimumSources || pass.contradictions > 0
}

package com.waheed.artificerx.ai.vision

data class ReferenceResearchPlan(val query:String,val visualQueries:List<String>,val searchQueries:List<String>,val requiredEvidence:List<String>,val avoidSources:List<String> = emptyList())

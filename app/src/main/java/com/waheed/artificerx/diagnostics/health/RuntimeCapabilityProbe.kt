package com.waheed.artificerx.diagnostics.health

class RuntimeCapabilityProbe(private val probes:Map<String,suspend()->Boolean>) {
    suspend fun run():List<CapabilityHealth> = probes.map { (name,probe)->val start=System.nanoTime();val ok=runCatching{probe()}.getOrDefault(false);CapabilityHealth(name,ok,(System.nanoTime()-start)/1_000_000,"runtime probe") }
}

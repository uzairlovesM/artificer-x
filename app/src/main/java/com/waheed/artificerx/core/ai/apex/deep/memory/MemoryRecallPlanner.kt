package com.waheed.artificerx.core.ai.apex.deep.memory

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class MemoryRecallPlannerInput(
    val id: String,
    val values: List<Double>,
    val weight: Double = 1.0,
    val priority: Int = 0,
    val tags: Set<String> = emptySet(),
)

data class MemoryRecallPlannerResult(
    val id: String,
    val score: Double,
    val normalized: List<Double>,
    val selected: Boolean,
    val reasons: List<String>,
    val diagnostics: Map<String, Double>,
)

class MemoryRecallPlanner(
    private val sensitivity: Double = 1.0,
    private val floor: Double = 0.000001,
) {
    private val history = ArrayDeque<MemoryRecallPlannerResult>()
    private val counters = LinkedHashMap<String, Long>()
    private val cache = LinkedHashMap<String, MemoryRecallPlannerResult>()

    fun evaluate(input: MemoryRecallPlannerInput): MemoryRecallPlannerResult {
        counters[input.id] = (counters[input.id] ?: 0L) + 1L
        val cacheKey = key(input)
        cache[cacheKey]?.let { history.add(it); trim(); return it }
        val normalized = normalize(input.values)
        val mean = normalized.averageOrZero()
        val variance = variance(normalized, mean)
        val stability = 1.0 / (1.0 + sqrt(variance))
        val signal = signal(normalized)
        val priorityBoost = (input.priority.coerceIn(-10, 10) / 10.0)
        val score = (0.36 * mean + 0.27 * stability + 0.25 * signal + 0.12 * priorityBoost) *
            input.weight.coerceIn(0.0, 4.0) * sensitivity
        val selected = score >= 0.42 && normalized.isNotEmpty()
        val reasons = buildList {
            add(if (normalized.isEmpty()) "empty" else "observed=${normalized.size}")
            add("mean=${format(mean)}")
            add("variance=${format(variance)}")
            add(if (selected) "selected" else "rejected")
            if ("urgent" in input.tags) add("urgent")
            if ("vision" in input.tags) add("vision-aware")
            if ("tool" in input.tags) add("tool-aware")
        }
        val result = MemoryRecallPlannerResult(
            id = input.id, score = score.coerceIn(-4.0, 4.0), normalized = normalized,
            selected = selected, reasons = reasons, diagnostics = mapOf(
                "mean" to mean, "variance" to variance, "stability" to stability,
                "signal" to signal, "priority" to priorityBoost, "calls" to (counters[input.id] ?: 0L).toDouble(),
            )
        )
        cache[cacheKey] = result
        history.add(result); trim(); return result
    }

    fun rank(inputs: List<MemoryRecallPlannerInput>): List<MemoryRecallPlannerResult> =
        inputs.map(::evaluate).sortedByDescending { it.score }

    fun select(inputs: List<MemoryRecallPlannerInput>, limit: Int): List<MemoryRecallPlannerResult> =
        rank(inputs).filter { it.selected }.take(limit.coerceAtLeast(0))

    fun aggregate(results: List<MemoryRecallPlannerResult>): MemoryRecallPlannerResult {
        if (results.isEmpty()) return MemoryRecallPlannerResult("aggregate", 0.0, emptyList(), false, listOf("empty"), emptyMap())
        val values = results.flatMap { it.normalized }
        val score = results.map { it.score }.average()
        val selected = results.count { it.selected } >= max(1, results.size / 2)
        return MemoryRecallPlannerResult(
            id = "aggregate", score = score, normalized = normalize(values), selected = selected,
            reasons = results.flatMap { it.reasons }.distinct().take(24),
            diagnostics = mapOf("items" to results.size.toDouble(), "selected" to results.count { it.selected }.toDouble())
        )
    }

    fun explain(id: String): List<String> = history.filter { it.id == id }.flatMap { it.reasons }.distinct()

    fun stats(): Map<String, Double> = mapOf(
        "history" to history.size.toDouble(),
        "cache" to cache.size.toDouble(),
        "keys" to counters.size.toDouble(),
        "calls" to counters.values.sum().toDouble(),
    )

    fun clearCache() { cache.clear() }

    private fun key(input: MemoryRecallPlannerInput): String = input.id + ":" + input.values.hashCode() + ":" + input.weight + ":" + input.priority

    private fun normalize(values: List<Double>): List<Double> {
        if (values.isEmpty()) return emptyList()
        val finite = values.map { if (it.isFinite()) it else 0.0 }
        val minValue = finite.minOrNull() ?: 0.0
        val maxValue = finite.maxOrNull() ?: 0.0
        val span = max(maxValue - minValue, floor)
        return finite.map { ((it - minValue) / span).coerceIn(0.0, 1.0) }
    }

    private fun variance(values: List<Double>, mean: Double): Double =
        if (values.isEmpty()) 0.0 else values.map { (it - mean) * (it - mean) }.average()

    private fun signal(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        var total = 0.0
        var previous = values.first()
        for (value in values.drop(1)) {
            total += abs(value - previous)
            previous = value
        }
        return 1.0 / (1.0 + total / max(values.size - 1, 1))
    }

    private fun trim() {
        while (history.size > 512) history.removeFirst()
        while (cache.size > 512) cache.remove(cache.keys.first())
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average().coerceIn(-1.0, 1.0)

    private fun format(value: Double): String = "%.5f".format(value)
}

class MemoryRecallPlannerStage1(private val bias: Double = 0.05) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}

private fun Double.tanhSafe(): Double = kotlin.math.tanh(this.coerceIn(-20.0, 20.0))

class MemoryRecallPlannerStage2(private val bias: Double = 0.1) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage3(private val bias: Double = 0.15) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage4(private val bias: Double = 0.2) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage5(private val bias: Double = 0.25) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage6(private val bias: Double = 0.3) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage7(private val bias: Double = 0.35) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage8(private val bias: Double = 0.4) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage9(private val bias: Double = 0.45) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage10(private val bias: Double = 0.5) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage11(private val bias: Double = 0.55) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage12(private val bias: Double = 0.6) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage13(private val bias: Double = 0.65) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


class MemoryRecallPlannerStage14(private val bias: Double = 0.7) {
    private val state = LinkedHashMap<String, Double>()
    fun transform(values: List<Double>, multiplier: Double = 1.0): List<Double> {
        val safe = values.map { if (it.isFinite()) it else 0.0 }
        val out = ArrayList<Double>(safe.size)
        var carry = bias
        safe.forEachIndexed { index, value ->
            val drift = kotlin.math.sin((index + 1) * (bias + 0.17)) * 0.05
            val next = ((value * multiplier) + carry + drift).tanhSafe()
            out += next
            carry = next * bias
        }
        state["count"] = (state["count"] ?: 0.0) + safe.size
        state["last"] = out.lastOrNull() ?: 0.0
        return out
    }
    fun merge(a: List<Double>, b: List<Double>): List<Double> {
        val size = max(a.size, b.size)
        return List(size) { index ->
            val av = a.getOrElse(index) { 0.0 }
            val bv = b.getOrElse(index) { 0.0 }
            ((av + bv) / 2.0 + bias).tanhSafe()
        }
    }
    fun score(values: List<Double>): Double = transform(values).let { if (it.isEmpty()) 0.0 else it.average() }
    fun snapshot(): Map<String, Double> = state.toMap()
    fun reset() { state.clear() }
}


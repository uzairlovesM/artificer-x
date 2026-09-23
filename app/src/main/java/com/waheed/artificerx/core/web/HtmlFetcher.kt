package com.waheed.artificerx.core.web

import net.dankito.readability4j.Readability4J
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Result of fetching + extracting readable content from a URL —
 *  Section: Web search/fetch tools. Mirrors the shape of a successful
 *  web_fetch call in this app's own agent conversation, giving the
 *  Reasoning Brain a title + clean article text rather than raw HTML
 *  it would have to visually parse out of a vision-feedback
 *  screenshot. */
sealed class WebFetchResult {
    data class Success(
        val url: String,
        val title: String?,
        val byline: String?,
        val readableText: String,
        val rawHtmlLengthBytes: Int,
    ) : WebFetchResult()

    data class HttpError(
        val url: String,
        val statusCode: Int,
        val message: String,
    ) : WebFetchResult()

    data class NetworkError(
        val url: String,
        val message: String,
    ) : WebFetchResult()

    data class ExtractionFailed(
        val url: String,
        val message: String,
    ) : WebFetchResult()

    data class Blocked(
        val url: String,
        val reason: String,
    ) : WebFetchResult()
}

/**
 * Real on-device web fetching, backing a new `web_fetch` agent tool
 * (Section: Web search/fetch tools). Two-stage pipeline: OkHttp does
 * the HTTP GET, readability4j (a Kotlin port of Mozilla's
 * Readability.js — the same algorithm behind Firefox's Reader View)
 * strips the response HTML down to just the article's readable
 * content, so what reaches the LLM's context window is dense signal
 * rather than markup noise.
 *
 * Deliberately NOT a general-purpose scraper: blocks common
 * SSRF-adjacent targets (localhost, link-local, private IP ranges)
 * before ever issuing the request, since this tool is reachable from
 * agent-directed tool calls and a model-driven "fetch
 * http://169.254.169.254/..." should never silently succeed.
 */
@Singleton
class HtmlFetcher
    @Inject
    constructor() {
        private val client =
            OkHttpClient
                .Builder()
                .connectTimeout(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

        suspend fun fetch(url: String): WebFetchResult {
            var currentUrl = url.trim()
            repeat(MAX_REDIRECTS + 1) { hop ->
                val blockedReason = blockedUrlReason(currentUrl)
                if (blockedReason != null) return WebFetchResult.Blocked(currentUrl, blockedReason)

                val request =
                    runCatching {
                        Request
                            .Builder()
                            .url(currentUrl)
                            .header("User-Agent", USER_AGENT)
                            .build()
                    }.getOrElse {
                        return WebFetchResult.NetworkError(currentUrl, "Malformed URL: ${it.message}")
                    }

                val response =
                    runCatching { client.newCall(request).execute() }
                        .getOrElse { return WebFetchResult.NetworkError(currentUrl, it.message ?: "Request failed") }

                response.use { resp ->
                    if (resp.code in 300..399) {
                        if (hop >= MAX_REDIRECTS) return WebFetchResult.NetworkError(currentUrl, "Too many redirects")
                        val location = resp.header("Location") ?: return WebFetchResult.NetworkError(currentUrl, "Redirect missing Location header")
                        currentUrl = runCatching { java.net.URI(currentUrl).resolve(location).toString() }
                            .getOrElse { return WebFetchResult.NetworkError(currentUrl, "Invalid redirect target") }
                        return@use
                    }
                    if (!resp.isSuccessful) return WebFetchResult.HttpError(currentUrl, resp.code, resp.message)
                    val body = resp.body ?: return WebFetchResult.ExtractionFailed(currentUrl, "Empty response body")
                    if (body.contentLength() > MAX_HTML_BYTES) {
                        return WebFetchResult.ExtractionFailed(currentUrl, "Response exceeds ${MAX_HTML_BYTES / (1024 * 1024)} MB safety limit")
                    }
                    val html = body.charStream().buffered().use { reader ->
                        val out = StringBuilder()
                        val buffer = CharArray(16 * 1024)
                        while (out.length < MAX_HTML_CHARS) {
                            val n = reader.read(buffer, 0, minOf(buffer.size, MAX_HTML_CHARS - out.length))
                            if (n < 0) break
                            out.append(buffer, 0, n)
                        }
                        out.toString()
                    }
                    return extractReadableContent(currentUrl, html)
                }
            }
            return WebFetchResult.NetworkError(currentUrl, "Redirect resolution failed")
        }

        private fun extractReadableContent(
            url: String,
            html: String,
        ): WebFetchResult =
            runCatching {
                val readability = Readability4J(url, html)
                val article = readability.parse()
                val text = article.textContent?.trim()
                if (text.isNullOrBlank()) {
                    return WebFetchResult.ExtractionFailed(url, "Readability extraction produced no content")
                }
                WebFetchResult.Success(
                    url = url,
                    title = article.title,
                    byline = article.byline,
                    readableText = text.take(MAX_EXTRACTED_TEXT_CHARS),
                    rawHtmlLengthBytes = html.toByteArray().size,
                )
            }.getOrElse { WebFetchResult.ExtractionFailed(url, it.message ?: "Extraction threw") }

        /** SSRF-adjacent guard: an agent-directed fetch tool is a
         *  meaningfully different trust boundary than a person typing a
         *  URL into a browser — the LLM chooses the URL, so this refuses
         *  to resolve requests aimed at loopback, link-local (including
         *  the common 169.254.169.254 cloud-metadata address), and
         *  private RFC1918 ranges before a connection is ever attempted. */
        private fun blockedUrlReason(url: String): String? {
            val host =
                runCatching { java.net.URI(url).host }.getOrNull()?.lowercase()
                    ?: return "Could not parse host from URL"
            val blockedHosts = setOf("localhost", "127.0.0.1", "0.0.0.0", "169.254.169.254", "::1")
            if (host in blockedHosts) return "Refusing to fetch a loopback/link-local/metadata address"
            if (host.startsWith("192.168.") || host.startsWith("10.") || isPrivate172Range(host)) {
                return "Refusing to fetch a private-network address"
            }
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                return "Only http/https URLs are supported"
            }
            return null
        }

        private fun isPrivate172Range(host: String): Boolean {
            if (!host.startsWith("172.")) return false
            val secondOctet = host.removePrefix("172.").substringBefore('.').toIntOrNull() ?: return false
            return secondOctet in 16..31
        }

        private companion object {
            const val FETCH_TIMEOUT_SECONDS = 15L
            const val MAX_REDIRECTS = 4
            const val MAX_HTML_BYTES = 2L * 1024L * 1024L
            const val MAX_HTML_CHARS = 2_000_000
            const val MAX_EXTRACTED_TEXT_CHARS = 12_000
            const val USER_AGENT = "ArtificerX/1.0 (personal-use agent)"
        }
    }

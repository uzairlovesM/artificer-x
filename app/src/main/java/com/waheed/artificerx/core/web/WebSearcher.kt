package com.waheed.artificerx.core.web

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** One organic result from a search — Section: Web search & fetch
 *  tools. Deliberately small (title/url/snippet only, no ranking
 *  metadata the model doesn't need) so a search result list stays
 *  cheap in the conversation's token budget across the many searches
 *  Deep Studio mode's research phase calls for. */
data class WebSearchResultItem(
    val title: String,
    val url: String,
    val snippet: String,
)

sealed class WebSearchResult {
    data class Success(
        val query: String,
        val results: List<WebSearchResultItem>,
    ) : WebSearchResult()

    data class NetworkError(
        val query: String,
        val message: String,
    ) : WebSearchResult()

    data class NoResults(
        val query: String,
    ) : WebSearchResult()
}

/**
 * Real `web_search` tool backing (Section: Web search & fetch tools) —
 * this is the actual research capability Deep Studio mode's system
 * prompt requires the model to use before drawing anything, not a
 * placeholder. No API key, no paid quota, no signup: scrapes
 * DuckDuckGo's plain HTML results endpoint
 * (html.duckduckgo.com/html/), which has stayed structurally stable
 * for years specifically because it exists as DDG's no-JS fallback UI
 * rather than a scraping target that gets actively defended against —
 * a deliberate choice for a zero-budget personal app over any paid
 * search API (Bing/Google/Exa/Tavily all require a key and a billing
 * account, which doesn't fit 0 from this project's own
 * README).
 *
 * Parsed with Jsoup (already a project dependency for HtmlFetcher)
 * rather than regex — DDG's result markup wraps each hit in a
 * `.result` block with `.result__a` (title+link) and `.result__snippet`
 * children, and Jsow's CSS-selector API is both more robust and more
 * readable against markup drift than hand-written regex would be.
 */
@Singleton
class WebSearcher
    @Inject
    constructor() {
        private val client =
            OkHttpClient
                .Builder()
                .connectTimeout(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

        suspend fun search(
            query: String,
            maxResults: Int = 6,
        ): WebSearchResult {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) return WebSearchResult.NoResults("")
            val safeMaxResults = maxResults.coerceIn(1, 20)
            val request =
                runCatching {
                    Request
                        .Builder()
                        .url("https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(normalizedQuery, "UTF-8")}")
                        .header("User-Agent", USER_AGENT)
                        .build()
                }.getOrElse {
                    return WebSearchResult.NetworkError(normalizedQuery, "Could not build search request: ${it.message}")
                }

            val response =
                runCatching { client.newCall(request).execute() }
                    .getOrElse { return WebSearchResult.NetworkError(normalizedQuery, it.message ?: "Search request failed") }

            return response.use { resp ->
                if (!resp.isSuccessful) {
                    return WebSearchResult.NetworkError(normalizedQuery, "HTTP ${resp.code}")
                }
                val body = resp.body ?: return WebSearchResult.NetworkError(normalizedQuery, "Empty search response")
                val html = body.charStream().buffered().use { reader ->
                    val out = StringBuilder()
                    val buffer = CharArray(16 * 1024)
                    while (out.length < MAX_RESPONSE_CHARS) {
                        val n = reader.read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_CHARS - out.length))
                        if (n < 0) break
                        out.append(buffer, 0, n)
                    }
                    out.toString()
                }
                parseResults(normalizedQuery, html, safeMaxResults)
            }
        }

        private fun parseResults(
            query: String,
            html: String,
            maxResults: Int,
        ): WebSearchResult =
            runCatching {
                val document = Jsoup.parse(html)
                val items =
                    document.select("div.result, div.web-result").mapNotNull { block ->
                        val linkEl = block.selectFirst("a.result__a") ?: return@mapNotNull null
                        val title = linkEl.text().trim()
                        // DDG's HTML result links go through a
                        // /l/?uddg=<encoded-real-url> redirect wrapper
                        // rather than the destination URL directly —
                        // unwrap it so the model (and any follow-up
                        // web_fetch call) gets the real target, not
                        // DDG's redirector.
                        val rawHref = linkEl.attr("href")
                        val realUrl = unwrapDuckDuckGoRedirect(rawHref)
                        val snippet = block.selectFirst(".result__snippet")?.text()?.trim().orEmpty()
                        val normalizedUrl = realUrl.trim()
                        if (title.isBlank() || normalizedUrl.isBlank() || !normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
                            null
                        } else {
                            WebSearchResultItem(title = title, url = normalizedUrl, snippet = snippet)
                        }
                    }.take(maxResults)

                if (items.isEmpty()) WebSearchResult.NoResults(query) else WebSearchResult.Success(query, items)
            }.getOrElse { WebSearchResult.NetworkError(query, "Result parsing failed: ${it.message}") }

        private fun unwrapDuckDuckGoRedirect(href: String): String {
            if (!href.contains("uddg=")) return if (href.startsWith("http")) href else "https:$href"
            val encoded = href.substringAfter("uddg=").substringBefore('&')
            return runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(href)
        }

        private companion object {
            const val SEARCH_TIMEOUT_SECONDS = 15L
            const val MAX_RESPONSE_CHARS = 2_000_000
            const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) ArtificerX/1.0 (personal-use agent)"
        }
    }

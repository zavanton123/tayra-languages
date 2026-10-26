package com.tayra.languages.backend

import com.tayra.languages.sync.ApiError
import com.tayra.languages.sync.ApiErrors
import com.tayra.languages.sync.SyncJson
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * How many requests one address may make a minute: few to sign up, sign in and ask for codes, which
 * guessing would use; many for everything else. Counted in memory, as one instance serves the
 * apps; behind the platform's proxy the address is the client's (server.forward-headers-strategy).
 */
@Configuration
class RateLimits(
    @Value("\${tayra.rate-limits.auth-per-minute}") private val authPerMinute: Int,
    @Value("\${tayra.rate-limits.other-per-minute}") private val otherPerMinute: Int,
    private val clock: Clock,
) {
    @Bean
    fun rateLimitFilter(): FilterRegistrationBean<RateLimitFilter> = FilterRegistrationBean(RateLimitFilter(authPerMinute, otherPerMinute, clock)).apply {
        addUrlPatterns("/api/*")
        order = Ordered.HIGHEST_PRECEDENCE
    }
}

class RateLimitFilter(private val authPerMinute: Int, private val otherPerMinute: Int, private val clock: Clock) : OncePerRequestFilter() {
    /** Requests in the current minute, by address and kind. */
    private class Window(val minute: Long, var count: Int)

    private val windows = ConcurrentHashMap<String, Window>()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val auth = request.requestURI.startsWith("/api/v1/auth/")
        val limit = if (auth) authPerMinute else otherPerMinute
        val minute = clock.millis() / 60_000
        if (windows.size > MAX_ADDRESSES) windows.entries.removeIf { it.value.minute < minute }
        val window = windows.compute("${if (auth) "auth" else "other"} ${request.remoteAddr}") { _, w ->
            if (w == null || w.minute != minute) Window(minute, 1) else w.apply { count++ }
        }!!
        if (window.count <= limit) return chain.doFilter(request, response)
        response.status = 429
        response.setHeader("Retry-After", (60 - clock.millis() / 1000 % 60).toString())
        response.contentType = "application/json"
        response.writer.write(SyncJson.encodeToString(ApiError.serializer(), ApiError(ApiErrors.RATE_LIMITED, "Too many requests; try again in a minute")))
    }

    private companion object {
        const val MAX_ADDRESSES = 10_000
    }
}

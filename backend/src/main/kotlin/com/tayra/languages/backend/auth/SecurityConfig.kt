package com.tayra.languages.backend.auth

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authorization.AuthorizationDecision
import java.security.MessageDigest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import javax.crypto.spec.SecretKeySpec

/**
 * Requests carry the access token as a bearer token; signing up and in, the service's clock and its
 * health need none, and its metrics the admin token. No cookies and no server sessions: every request stands on its token.
 */
@Configuration
class SecurityConfig(@Value("\${tayra.auth.jwt-secret}") secret: String, @Value("\${tayra.admin-token}") private val adminToken: String) {
    private val key = SecretKeySpec(secret.toByteArray().also { require(it.size >= 32) { "JWT_SECRET needs at least 32 bytes" } }, "HmacSHA256")

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/v1/auth/**", "/api/v1/info", "/actuator/health/**", "/actuator/health", "/error").permitAll()
            // The owner's own look at how the service does, with a token of its own.
            it.requestMatchers("/actuator/metrics", "/actuator/metrics/**").access { _, context ->
                val sent = context.request.getHeader("X-Admin-Token").orEmpty()
                AuthorizationDecision(adminToken.isNotEmpty() && MessageDigest.isEqual(sent.toByteArray(), adminToken.toByteArray()))
            }
            it.anyRequest().authenticated()
        }
        .oauth2ResourceServer { it.jwt { } }
        .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
        .build()

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(key))

    @Bean
    fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}

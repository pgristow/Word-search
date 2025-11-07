package com.wordsearch.config

import com.wordsearch.repository.UserRepository
import com.wordsearch.util.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            try {
                // Extract userId and username from token
                val userId = jwtUtil.extractUserId(token)
                val username = jwtUtil.extractUsername(token)

                // Validate token is not expired
                if (!jwtUtil.isTokenExpired(token)) {
                    // Verify user exists
                    val userExists = userRepository.existsById(java.util.UUID.fromString(userId))

                    if (userExists) {
                        // Create authentication token with userId as principal
                        val authentication = UsernamePasswordAuthenticationToken(
                            userId, // Principal will be userId string
                            null,
                            emptyList() // No authorities for now
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                        // Set authentication in security context
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            } catch (e: Exception) {
                // Invalid token, continue without authentication
                logger.debug("JWT token validation failed: ${e.message}")
            }
        }

        filterChain.doFilter(request, response)
    }
}

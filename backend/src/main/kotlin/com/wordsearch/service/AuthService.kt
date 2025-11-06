package com.wordsearch.service

import com.wordsearch.dto.AuthResponse
import com.wordsearch.dto.LoginRequest
import com.wordsearch.dto.RegisterRequest
import com.wordsearch.model.User
import com.wordsearch.model.UserProgress
import com.wordsearch.repository.UserProgressRepository
import com.wordsearch.repository.UserRepository
import com.wordsearch.util.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userProgressRepository: UserProgressRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        // Check if username already exists
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        // Create new user
        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )

        val savedUser = userRepository.save(user)

        // Create initial user progress
        val userProgress = UserProgress(userId = savedUser.id)
        userProgressRepository.save(userProgress)

        // Generate JWT token
        val token = jwtUtil.generateToken(savedUser.id.toString(), savedUser.username)

        return AuthResponse(
            token = token,
            userId = savedUser.id.toString(),
            username = savedUser.username,
            email = savedUser.email
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        // Find user by username
        val user = userRepository.findByUsername(request.username)
            ?: throw IllegalArgumentException("Invalid username or password")

        // Verify password
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password")
        }

        // Generate JWT token
        val token = jwtUtil.generateToken(user.id.toString(), user.username)

        return AuthResponse(
            token = token,
            userId = user.id.toString(),
            username = user.username,
            email = user.email
        )
    }
}

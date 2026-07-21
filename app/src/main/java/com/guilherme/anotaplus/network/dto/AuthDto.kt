package com.guilherme.anotaplus.network.dto

data class GoogleLoginRequest(val idToken: String)

data class AuthResponse(val accessToken: String, val user: UserDto)

data class UserDto(val id: String, val email: String, val name: String?, val pro: Boolean = false)

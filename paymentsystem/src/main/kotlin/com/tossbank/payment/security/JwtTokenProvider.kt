package com.tossbank.payment.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private var jwtExpirationInMs: Long = 0

    private fun getSigningKey(): Key {
        val keyBytes = jwtSecret.toByteArray()
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetails
        val expiryDate = Date(System.currentTimeMillis() + jwtExpirationInMs)

        return Jwts.builder()
            .setSubject(userPrincipal.username)
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun generateTokenFromUserId(userId: Long): String {
        val expiryDate = Date(System.currentTimeMillis() + jwtExpirationInMs)

        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body

        return claims.subject.toLong()
    }

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken)
            return true
        } catch (ex: SecurityException) {
            // Invalid JWT signature
        } catch (ex: MalformedJwtException) {
            // Invalid JWT token
        } catch (ex: ExpiredJwtException) {
            // Expired JWT token
        } catch (ex: UnsupportedJwtException) {
            // Unsupported JWT token
        } catch (ex: IllegalArgumentException) {
            // JWT claims string is empty
        }
        return false
    }

    fun getExpirationDateFromToken(token: String): Date {
        val claims = Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body

        return claims.expiration
    }

    fun canTokenBeRefreshed(token: String): Boolean {
        return !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        val expiration = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }
}
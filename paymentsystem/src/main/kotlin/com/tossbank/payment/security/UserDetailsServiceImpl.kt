package com.tossbank.payment.security

import com.tossbank.payment.entity.User
import com.tossbank.payment.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDetailsServiceImpl : UserDetailsService {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = when {
            username.contains("@") -> {
                userRepository.findActiveUserByEmail(username)
            }
            username.toLongOrNull() != null -> {
                userRepository.findById(username.toLong()).orElse(null)
            }
            else -> {
                userRepository.findByPhoneNumber(username)
            }
        } ?: throw UsernameNotFoundException("User not found with identifier: $username")

        return UserPrincipal.create(user)
    }
}

data class UserPrincipal(
    val id: Long,
    val name: String,
    private val username: String,
    val email: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getUsername(): String = username
    override fun getPassword(): String = password
    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun create(user: User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            
            return UserPrincipal(
                id = user.id,
                name = user.name,
                username = user.email,
                email = user.email,
                password = user.password,
                authorities = authorities
            )
        }
    }
}
package org.seed.aimenu.application.service

import org.seed.aimenu.application.port.output.MenuStore
import org.seed.aimenu.domain.model.*
import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Service
class AuthService(private val store: MenuStore) {
    private val encoder=BCryptPasswordEncoder(12)
    private fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){
        "%02x".format(it)
    }
    private fun fail(message:String,status:Int=400):Nothing=throw BusinessException(status,message)
    private fun email(c:Credentials)=c.email.trim().lowercase().also {
        if(it.length>254 || !it.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) fail("Invalid email")
    }
    fun token(user:Long):Map<String,Any> {
        val token=Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also{
            SecureRandom().nextBytes(it)
        })
        store.session(user,hash(token))
        return mapOf("token" to token,"userId" to user,"expiresIn" to 604800)
    }
    fun createAccount(c: Credentials): Long {
        if(c.password.length<10 || c.password.toByteArray().size>72) fail("Password must be 10 to 72 bytes")
        val user=store.register(email(c),requireNotNull(encoder.encode(c.password)))
        return user
    }
    fun login(c:Credentials):Map<String,Any> {
        if(c.password.toByteArray().size>72) fail("Invalid credentials",401)
        val account=store.account(email(c)) ?: fail("Invalid credentials",401)
        if(!encoder.matches(c.password,account.second)) fail("Invalid credentials",401)
        return token(account.first)
    }
    fun authenticate(header:String?):Long {
        if(header==null || !header.startsWith("Bearer ")) fail("Bearer token required",401)
        return store.authenticate(hash(header.removePrefix("Bearer "))) ?: fail("Invalid or expired token",401)
    }
}

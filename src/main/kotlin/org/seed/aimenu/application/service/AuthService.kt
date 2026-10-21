package org.seed.aimenu.application.service

import org.seed.aimenu.application.port.output.MenuStore
import org.seed.aimenu.domain.exception.BusinessException
import org.seed.aimenu.domain.model.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class AuthService(private val store: MenuStore, @Value("\${jwt.secret}") secret: String) {
    private val encoder = BCryptPasswordEncoder(12)
    private val secretBytes = secret.toByteArray(StandardCharsets.UTF_8).also {
        require(it.size >= 32) { "JWT_SECRET must contain at least 32 UTF-8 bytes" }
    }
    private val expirySeconds = 604800L
    private val base64 = Base64.getUrlEncoder().withoutPadding()
    private fun fail(message: String, status: Int = 400): Nothing = throw BusinessException(status, message)
    private fun email(c: Credentials) = c.email.trim().lowercase().also {
        if (it.length > 254 || !it.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) fail("Некорректный адрес электронной почты")
    }
    private fun sign(input: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
        return base64.encodeToString(mac.doFinal(input.toByteArray(StandardCharsets.US_ASCII)))
    }
    fun token(user: UUID): Map<String, Any> {
        val now = Instant.now().epochSecond
        val header = base64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".toByteArray(StandardCharsets.UTF_8))
        val payload = base64.encodeToString("{\"sub\":\"$user\",\"iat\":$now,\"exp\":${now + expirySeconds}}".toByteArray(StandardCharsets.UTF_8))
        val unsigned = "$header.$payload"
        return mapOf("token" to "$unsigned.${sign(unsigned)}", "userId" to user, "expiresIn" to expirySeconds)
    }
    fun createAccount(c: Credentials): UUID {
        if (c.password.length < 10 || c.password.toByteArray().size > 72) fail("Пароль должен содержать от 10 до 72 байт")
        return store.register(email(c), requireNotNull(encoder.encode(c.password)))
    }
    fun login(c: Credentials): Map<String, Any> {
        if (c.password.toByteArray().size > 72) fail("Неверный адрес или пароль", 401)
        val account = store.account(email(c)) ?: fail("Неверный адрес или пароль", 401)
        if (!encoder.matches(c.password, account.second)) fail("Неверный адрес или пароль", 401)
        return token(account.first)
    }
    fun authenticate(header: String?): UUID {
        if (header == null || header.length > 4096 || !header.startsWith("Bearer ")) fail("Для входа требуется токен", 401)
        val parts = header.removePrefix("Bearer ").split('.')
        if (parts.size != 3 || parts.any { it.isEmpty() }) fail("Токен недействителен или истёк", 401)
        val unsigned = "${parts[0]}.${parts[1]}"
        val expected = sign(unsigned).toByteArray(StandardCharsets.US_ASCII)
        if (!java.security.MessageDigest.isEqual(expected, parts[2].toByteArray(StandardCharsets.US_ASCII))) fail("Токен недействителен или истёк", 401)
        try {
            val jwtHeader = String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
            val payload = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            if (jwtHeader != "{\"alg\":\"HS256\",\"typ\":\"JWT\"}") fail("Токен недействителен или истёк", 401)
            val match = Regex("^\\{\\\"sub\\\":\\\"([0-9a-fA-F-]{36})\\\",\\\"iat\\\":([0-9]+),\\\"exp\\\":([0-9]+)\\}$").matchEntire(payload)
                ?: fail("Токен недействителен или истёк", 401)
            val issued = match.groupValues[2].toLong()
            val expires = match.groupValues[3].toLong()
            val now = Instant.now().epochSecond
            if (issued > now || expires <= now || expires - issued != expirySeconds) fail("Токен недействителен или истёк", 401)
            return UUID.fromString(match.groupValues[1])
        } catch (e: IllegalArgumentException) {
            fail("Токен недействителен или истёк", 401)
        }
    }
}

package org.seed.aimenu.presentation.exception
import org.seed.aimenu.domain.exception.BusinessException
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.converter.HttpMessageNotReadableException
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(BusinessException::class) fun business(e:BusinessException)=ResponseEntity.status(e.status).body(mapOf("error" to e.message))
    @ExceptionHandler(DataIntegrityViolationException::class) fun conflict(e:DataIntegrityViolationException)=ResponseEntity.status(409).body(mapOf("error" to "Данные конфликтуют или некорректны"))
    @ExceptionHandler(HttpMessageNotReadableException::class) fun invalid(e:HttpMessageNotReadableException)=ResponseEntity.badRequest().body(mapOf("error" to "Некорректные данные запроса"))
}

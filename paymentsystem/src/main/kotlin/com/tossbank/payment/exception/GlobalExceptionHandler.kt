package com.tossbank.payment.exception

import com.tossbank.payment.dto.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.sql.SQLException
import java.time.LocalDateTime

data class ErrorResponse(
    val success: Boolean = false,
    val message: String,
    val errorCode: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String,
    val details: Map<String, Any>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Custom Payment Platform Exceptions
    @ExceptionHandler(PaymentPlatformException::class)
    fun handlePaymentPlatformException(
        ex: PaymentPlatformException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Payment platform exception: {} - {}", ex.errorCode, ex.message, ex)
        
        val response = ErrorResponse(
            message = ex.message ?: "Unknown error occurred",
            errorCode = ex.errorCode,
            path = request.requestURI,
            details = when (ex) {
                is InsufficientBalanceException -> mapOf(
                    "requestedAmount" to ex.requestedAmount,
                    "availableBalance" to ex.availableBalance
                )
                is AccountLimitExceededException -> mapOf(
                    "limitType" to ex.limitType,
                    "limit" to ex.limit,
                    "requestedAmount" to ex.requestedAmount
                )
                is InvalidPaymentStatusException -> mapOf(
                    "currentStatus" to ex.currentStatus,
                    "requiredStatus" to ex.requiredStatus
                )
                is RateLimitExceededException -> mapOf(
                    "limitType" to ex.limitType,
                    "retryAfterSeconds" to ex.retryAfterSeconds
                )
                is ValidationException -> mapOf(
                    "fieldErrors" to ex.fieldErrors
                )
                is ExternalServiceException -> mapOf(
                    "serviceName" to ex.serviceName
                )
                is ResourceNotFoundException -> mapOf(
                    "resourceType" to ex.resourceType,
                    "resourceId" to ex.resourceId
                )
                else -> null
            }
        )
        
        return ResponseEntity.status(ex.httpStatus).body(response)
    }

    // Spring Security Exceptions
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Bad credentials: {}", ex.message)
        
        val response = ErrorResponse(
            message = "Invalid email or password",
            errorCode = "AUTH_INVALID_CREDENTIALS",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Authentication exception: {}", ex.message, ex)
        
        val response = ErrorResponse(
            message = "Authentication failed",
            errorCode = "AUTH_FAILED",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: {}", ex.message)
        
        val response = ErrorResponse(
            message = "Access denied",
            errorCode = "AUTH_ACCESS_DENIED",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    // Validation Exceptions
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception: {}", ex.message)
        
        val fieldErrors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        
        val response = ErrorResponse(
            message = "Validation failed",
            errorCode = "VALIDATION_ERROR",
            path = request.requestURI,
            details = mapOf("fieldErrors" to fieldErrors)
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch exception: {}", ex.message)
        
        val response = ErrorResponse(
            message = "Invalid parameter type: ${ex.name}",
            errorCode = "REQUEST_PARAMETER_INVALID",
            path = request.requestURI,
            details = mapOf(
                "parameter" to (ex.name ?: "unknown"),
                "expectedType" to (ex.requiredType?.simpleName ?: "unknown"),
                "actualValue" to (ex.value ?: "null")
            )
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Message not readable exception: {}", ex.message)
        
        val response = ErrorResponse(
            message = "Invalid request format or malformed JSON",
            errorCode = "REQUEST_FORMAT_INVALID",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Method not supported exception: {}", ex.message)
        
        val response = ErrorResponse(
            message = "HTTP method ${ex.method} not supported for this endpoint",
            errorCode = "METHOD_NOT_SUPPORTED",
            path = request.requestURI,
            details = mapOf(
                "method" to (ex.method ?: "unknown"),
                "supportedMethods" to (ex.supportedMethods?.joinToString(", ") ?: "none")
            )
        )
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response)
    }

    // Database Exceptions
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Data integrity violation: {}", ex.message, ex)
        
        val message = when {
            ex.message?.contains("Duplicate entry") == true -> "Duplicate entry - this record already exists"
            ex.message?.contains("foreign key constraint") == true -> "Cannot perform operation - referenced data exists"
            ex.message?.contains("NOT NULL") == true -> "Required field cannot be empty"
            else -> "Data integrity violation"
        }
        
        val response = ErrorResponse(
            message = message,
            errorCode = "DATABASE_INTEGRITY_VIOLATION",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    @ExceptionHandler(SQLException::class)
    fun handleSQLException(
        ex: SQLException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("SQL exception: {}", ex.message, ex)
        
        val response = ErrorResponse(
            message = "Database operation failed",
            errorCode = "DATABASE_ERROR",
            path = request.requestURI,
            details = mapOf(
                "sqlState" to ex.sqlState,
                "errorCode" to ex.errorCode
            )
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    // Generic Exceptions
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument exception: {}", ex.message)
        
        val response = ErrorResponse(
            message = ex.message ?: "Invalid argument provided",
            errorCode = "INVALID_ARGUMENT",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal state exception: {}", ex.message)
        
        val response = ErrorResponse(
            message = ex.message ?: "Invalid operation state",
            errorCode = "INVALID_STATE",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        ex: RuntimeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled runtime exception: {}", ex.message, ex)
        
        val response = ErrorResponse(
            message = "An unexpected error occurred",
            errorCode = "INTERNAL_SERVER_ERROR",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    // Catch-all Exception Handler
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: {}", ex.message, ex)
        
        val response = ErrorResponse(
            message = "An unexpected system error occurred",
            errorCode = "SYSTEM_ERROR",
            path = request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
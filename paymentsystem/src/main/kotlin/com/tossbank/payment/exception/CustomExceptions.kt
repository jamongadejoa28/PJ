package com.tossbank.payment.exception

// Base Payment Platform Exception
abstract class PaymentPlatformException(
    message: String,
    val errorCode: String,
    val httpStatus: Int = 400,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Authentication & Authorization Exceptions
class InvalidCredentialsException(message: String = "Invalid email or password") : 
    PaymentPlatformException(message, "AUTH_INVALID_CREDENTIALS", 401)

class AccountLockedException(message: String = "Account has been locked due to too many failed attempts") : 
    PaymentPlatformException(message, "AUTH_ACCOUNT_LOCKED", 423)

class UnauthorizedAccessException(message: String = "Access denied") : 
    PaymentPlatformException(message, "AUTH_UNAUTHORIZED", 403)

class TokenExpiredException(message: String = "JWT token has expired") : 
    PaymentPlatformException(message, "AUTH_TOKEN_EXPIRED", 401)

// Account Management Exceptions
class AccountNotFoundException(message: String = "Account not found") : 
    PaymentPlatformException(message, "ACCOUNT_NOT_FOUND", 404)

class InsufficientBalanceException(
    message: String = "Insufficient account balance",
    val requestedAmount: java.math.BigDecimal,
    val availableBalance: java.math.BigDecimal
) : PaymentPlatformException(message, "ACCOUNT_INSUFFICIENT_BALANCE", 400)

class AccountLimitExceededException(
    message: String,
    val limitType: String,
    val limit: java.math.BigDecimal,
    val requestedAmount: java.math.BigDecimal
) : PaymentPlatformException(message, "ACCOUNT_LIMIT_EXCEEDED", 400)

class AccountFrozenException(message: String = "Account is frozen") : 
    PaymentPlatformException(message, "ACCOUNT_FROZEN", 423)

// Payment Processing Exceptions
class PaymentNotFoundException(message: String = "Payment not found") : 
    PaymentPlatformException(message, "PAYMENT_NOT_FOUND", 404)

class PaymentProcessingException(message: String, cause: Throwable? = null) : 
    PaymentPlatformException(message, "PAYMENT_PROCESSING_ERROR", 500, cause)

class PaymentAlreadyProcessedException(message: String = "Payment has already been processed") : 
    PaymentPlatformException(message, "PAYMENT_ALREADY_PROCESSED", 409)

class InvalidPaymentStatusException(
    message: String,
    val currentStatus: String,
    val requiredStatus: String
) : PaymentPlatformException(message, "PAYMENT_INVALID_STATUS", 400)

class PaymentMethodNotSupportedException(message: String) : 
    PaymentPlatformException(message, "PAYMENT_METHOD_NOT_SUPPORTED", 400)

class PaymentAmountInvalidException(message: String = "Payment amount is invalid") : 
    PaymentPlatformException(message, "PAYMENT_AMOUNT_INVALID", 400)

// Merchant API Exceptions
class MerchantNotFoundException(message: String = "Merchant not found") : 
    PaymentPlatformException(message, "MERCHANT_NOT_FOUND", 404)

class MerchantInactiveException(message: String = "Merchant is inactive") : 
    PaymentPlatformException(message, "MERCHANT_INACTIVE", 403)

class InvalidApiKeyException(message: String = "Invalid API key") : 
    PaymentPlatformException(message, "MERCHANT_INVALID_API_KEY", 401)

class MerchantOrderDuplicateException(message: String = "Duplicate merchant order ID") : 
    PaymentPlatformException(message, "MERCHANT_ORDER_DUPLICATE", 409)

class PaymentKeyNotFoundException(message: String = "Payment key not found") : 
    PaymentPlatformException(message, "MERCHANT_PAYMENT_KEY_NOT_FOUND", 404)

class PaymentKeyExpiredException(message: String = "Payment key has expired") : 
    PaymentPlatformException(message, "MERCHANT_PAYMENT_KEY_EXPIRED", 410)

// Rate Limiting & Security Exceptions
class RateLimitExceededException(
    message: String = "Rate limit exceeded",
    val limitType: String,
    val retryAfterSeconds: Int
) : PaymentPlatformException(message, "RATE_LIMIT_EXCEEDED", 429)

class SuspiciousActivityException(message: String = "Suspicious activity detected") : 
    PaymentPlatformException(message, "SECURITY_SUSPICIOUS_ACTIVITY", 403)

class IpBlockedException(message: String = "IP address has been blocked") : 
    PaymentPlatformException(message, "SECURITY_IP_BLOCKED", 403)

// Validation Exceptions
class ValidationException(
    message: String,
    val fieldErrors: Map<String, String> = emptyMap()
) : PaymentPlatformException(message, "VALIDATION_ERROR", 400)

class InvalidRequestFormatException(message: String = "Invalid request format") : 
    PaymentPlatformException(message, "REQUEST_FORMAT_INVALID", 400)

// External Service Exceptions
class ExternalServiceException(
    message: String,
    val serviceName: String,
    cause: Throwable? = null
) : PaymentPlatformException(message, "EXTERNAL_SERVICE_ERROR", 502, cause)

class ExternalServiceTimeoutException(
    message: String,
    val serviceName: String
) : PaymentPlatformException(message, "EXTERNAL_SERVICE_TIMEOUT", 504)

// Database & System Exceptions
class DatabaseConnectionException(message: String = "Database connection failed", cause: Throwable? = null) : 
    PaymentPlatformException(message, "DATABASE_CONNECTION_ERROR", 503, cause)

class SystemMaintenanceException(message: String = "System is under maintenance") : 
    PaymentPlatformException(message, "SYSTEM_MAINTENANCE", 503)

class ResourceNotFoundException(message: String, val resourceType: String, val resourceId: String) : 
    PaymentPlatformException(message, "RESOURCE_NOT_FOUND", 404)

// Concurrency Exceptions
class ConcurrentModificationException(message: String = "Resource was modified by another process") : 
    PaymentPlatformException(message, "CONCURRENT_MODIFICATION", 409)

class TransactionTimeoutException(message: String = "Transaction timed out") : 
    PaymentPlatformException(message, "TRANSACTION_TIMEOUT", 408)
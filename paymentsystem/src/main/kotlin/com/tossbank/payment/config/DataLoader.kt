package com.tossbank.payment.config

import com.tossbank.payment.entity.*
import com.tossbank.payment.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class MerchantProfile(
    val code: String,
    val name: String,
    val businessNumber: String,
    val representative: String,
    val phone: String,
    val email: String,
    val address: String,
    val category: MerchantCategory,
    val apiKey: String
)

@Component
class DataLoader : CommandLineRunner {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var merchantRepository: MerchantRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var paymentHistoryRepository: PaymentHistoryRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    override fun run(vararg args: String?) {
        // 이미 데이터가 있는지 확인
        if (userRepository.count() > 0) {
            println(" 테스트 데이터가 이미 존재합니다.")
            return
        }

        println(" 테스트 데이터 생성 시작...")

        // 1. 사용자 생성 (더 다양한 프로필)
        val users = mutableListOf<User>()
        
        val demoProfiles = listOf(
            Triple("demo@tossbank.com", "데모 사용자", "010-9999-8888"),
            Triple("user1@tossbank.com", "김토스", "010-1234-5678"),
            Triple("user2@tossbank.com", "이뱅크", "010-2345-6789"),
            Triple("premium@tossbank.com", "박프리미엄", "010-3456-7890"),
            Triple("student@tossbank.com", "정학생", "010-5678-9012"),
            Triple("freelancer@tossbank.com", "최프리랜서", "010-6789-0123")
        )
        
        demoProfiles.forEach { (email, name, phone) ->
            val user = User(
                email = email,
                password = passwordEncoder.encode("Demo123!"),
                name = name,
                phoneNumber = phone,
                status = UserStatus.ACTIVE
            )
            user.isEmailVerified = true
            user.isPhoneVerified = true
            users.add(userRepository.save(user))
        }

        println(" 사용자 ${users.size}명 생성 완료")

        // 2. 가맹점 생성 (더 다양한 업종)
        val merchants = mutableListOf<Merchant>()

        val merchantProfiles = listOf(
            MerchantProfile(
                "TOSS_STORE_001", "토스 온라인몰", "123-45-67890", "김사장",
                "02-1234-5678", "store@tossbank.com", "서울시 강남구 테헤란로 123",
                MerchantCategory.ONLINE, "toss_api_key_1234567890abcdef1234567890abcdef12345678"
            ),
            MerchantProfile(
                "TOSS_CAFE_002", "토스 카페", "234-56-78901", "박대표",
                "02-2345-6789", "cafe@tossbank.com", "서울시 서초구 강남대로 456",
                MerchantCategory.RESTAURANT, "toss_api_key_abcdef1234567890abcdef1234567890abcdef12"
            ),
            MerchantProfile(
                "TOSS_BOOK_003", "토스 서점", "345-67-89012", "정대리",
                "02-3456-7890", "book@tossbank.com", "서울시 마포구 월드컵로 789",
                MerchantCategory.RETAIL, "toss_api_key_fedcba0987654321fedcba0987654321fedcba09"
            ),
            MerchantProfile(
                "TOSS_GYM_004", "토스 피트니스", "456-78-90123", "최코치",
                "02-4567-8901", "gym@tossbank.com", "서울시 송파구 올림픽로 321",
                MerchantCategory.HEALTHCARE, "toss_api_key_1357902468acbdef1357902468acbdef13579024"
            )
        )
        
        merchantProfiles.forEach { profile ->
            val merchant = Merchant(
                merchantCode = profile.code,
                merchantName = profile.name,
                businessNumber = profile.businessNumber,
                representativeName = profile.representative,
                phoneNumber = profile.phone,
                email = profile.email,
                address = profile.address,
                category = profile.category,
                status = MerchantStatus.ACTIVE,
                apiKey = profile.apiKey,
                webhookUrl = "https://${profile.code.lowercase().replace("_", "")}.tossbank.com/webhook"
            )
            merchants.add(merchantRepository.save(merchant))
        }

        println(" 가맹점 ${merchants.size}개 생성 완료")

        // 3. 계좌 생성 (더 현실적인 잔액 분포)
        val accounts = mutableListOf<Account>()
        
        val accountBalances = listOf(
            BigDecimal("1500000.00"), // 데모 사용자 - 150만원
            BigDecimal("850000.00"),  // 김토스 - 85만원  
            BigDecimal("2750000.00"), // 이뱅크 - 275만원
            BigDecimal("5200000.00"), // 박프리미엄 - 520만원 (프리미엄)
            BigDecimal("320000.00"),  // 정학생 - 32만원 (학생)
            BigDecimal("1180000.00")  // 최프리랜서 - 118만원 (프리랜서)
        )
        
        val accountTypes = listOf(
            AccountType.CHECKING, AccountType.SAVINGS, AccountType.CHECKING,
            AccountType.CHECKING, AccountType.SAVINGS, AccountType.CHECKING
        )
        
        users.forEachIndexed { index, user ->
            val account = Account(
                accountNumber = "1000-01-${(123456 + index).toString()}",
                accountName = "${user.name} 주계좌",
                balance = accountBalances[index],
                accountType = accountTypes[index],
                status = AccountStatus.ACTIVE,
                dailyLimit = if (accountBalances[index] > BigDecimal("3000000")) 
                    BigDecimal("10000000.00") else BigDecimal("5000000.00"),
                monthlyLimit = if (accountBalances[index] > BigDecimal("3000000")) 
                    BigDecimal("100000000.00") else BigDecimal("50000000.00")
            )
            account.user = user
            accounts.add(accountRepository.save(account))
        }

        println(" 계좌 ${accounts.size}개 생성 완료")

        // 4. 결제 거래 생성 (더 다양한 시나리오)
        val payments = mutableListOf<Payment>()
        
        // 각종 결제 시나리오 데이터
        val paymentScenarios = listOf(
            // 성공 케이스들
            Triple("스마트폰 케이스 구매", BigDecimal("89000.00"), PaymentMethod.TOSS_PAY),
            Triple("아메리카노", BigDecimal("4500.00"), PaymentMethod.ACCOUNT_TRANSFER),
            Triple("자기계발서 3권", BigDecimal("45000.00"), PaymentMethod.CARD),
            Triple("헬스장 1개월 이용권", BigDecimal("120000.00"), PaymentMethod.TOSS_PAY),
            Triple("무선 이어폰", BigDecimal("159000.00"), PaymentMethod.ACCOUNT_TRANSFER),
            Triple("카페라떼 + 샌드위치", BigDecimal("12000.00"), PaymentMethod.TOSS_PAY),
            Triple("프리미엄 노트북 스탠드", BigDecimal("225000.00"), PaymentMethod.CARD),
            Triple("프로틴 보충제", BigDecimal("85000.00"), PaymentMethod.ACCOUNT_TRANSFER),
            // 실패/처리중 케이스들  
            Triple("태블릿 구매 시도", BigDecimal("750000.00"), PaymentMethod.CARD), // 한도초과로 실패
            Triple("샐러드 세트", BigDecimal("18000.00"), PaymentMethod.TOSS_PAY) // 잔액부족으로 실패
        )
        
        val merchantRotation = listOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1) // 가맹점 순환
        val userRotation = listOf(0, 1, 2, 3, 4, 5, 0, 1, 2, 3) // 사용자 순환
        val statusList = listOf(
            PaymentStatus.COMPLETED, PaymentStatus.COMPLETED, PaymentStatus.COMPLETED,
            PaymentStatus.COMPLETED, PaymentStatus.COMPLETED, PaymentStatus.COMPLETED,
            PaymentStatus.COMPLETED, PaymentStatus.COMPLETED,
            PaymentStatus.FAILED, PaymentStatus.FAILED
        )
        
        paymentScenarios.forEachIndexed { index, (description, amount, method) ->
            val payment = Payment(
                transactionId = "TXN_20250904_${(index + 1).toString().padStart(3, '0')}",
                merchantOrderId = "ORDER_20250904_${(index + 1).toString().padStart(3, '0')}",
                amount = amount,
                description = description,
                paymentMethod = method,
                status = statusList[index],
                clientIp = "192.168.1.${100 + index}",
                userAgent = "Mozilla/5.0 (TossBank Demo Client)",
                failureReason = if (statusList[index] == PaymentStatus.FAILED) {
                    if (amount > BigDecimal("500000")) "일일 한도 초과" else "잔액 부족"
                } else null
            )
            
            payment.user = users[userRotation[index]]
            payment.account = accounts[userRotation[index]]
            payment.merchant = merchants[merchantRotation[index]]
            
            if (statusList[index] == PaymentStatus.COMPLETED) {
                payment.approvedAt = LocalDateTime.now().minusHours((index + 1).toLong())
            }
            
            payments.add(paymentRepository.save(payment))
        }

        println(" 결제 거래 ${payments.size}건 생성 완료")

        // 5. 결제 이력 생성 (모든 결제에 대해)
        var historyCount = 0

        payments.forEach { payment ->
            val histories = when (payment.status) {
                PaymentStatus.COMPLETED -> listOf(
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "PENDING",
                        previousStatus = null,
                        description = "결제 요청 생성",
                        processedBy = "SYSTEM",
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment },
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "PROCESSING",
                        previousStatus = "PENDING", 
                        description = "${payment.paymentMethod} 결제 처리 시작",
                        processedBy = "SYSTEM",
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment },
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "COMPLETED",
                        previousStatus = "PROCESSING",
                        description = "결제 승인 완료",
                        processedBy = "SYSTEM",
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment }
                )
                
                PaymentStatus.FAILED -> listOf(
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "PENDING",
                        previousStatus = null,
                        description = "결제 요청 생성",
                        processedBy = "SYSTEM",
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment },
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "PROCESSING",
                        previousStatus = "PENDING",
                        description = "${payment.paymentMethod} 결제 처리 시작",
                        processedBy = "SYSTEM", 
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment },
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "FAILED",
                        previousStatus = "PROCESSING",
                        description = "결제 실패: ${payment.failureReason}",
                        processedBy = "SYSTEM",
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment }
                )
                
                else -> listOf(
                    PaymentHistory(
                        amount = payment.amount,
                        currentStatus = "PENDING",
                        previousStatus = null,
                        description = "결제 요청 생성",
                        processedBy = "SYSTEM",
                        clientIp = payment.clientIp
                    ).apply { this.payment = payment }
                )
            }
            
            histories.forEach { paymentHistoryRepository.save(it) }
            historyCount += histories.size
        }

        println(" 결제 이력 ${historyCount}건 생성 완료")

        println(" 테스트 데이터 생성 완료!")
        println("==================================")
        println(" 생성된 데이터 요약:")
        println("- 사용자: ${users.size}명")
        println("- 가맹점: ${merchants.size}개") 
        println("- 계좌: ${accounts.size}개")
        println("- 결제 거래: ${payments.size}건")
        println("- 결제 이력: ${historyCount}건")
        println("==================================")
    }
}
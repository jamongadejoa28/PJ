package com.tossbank.payment.config

import com.tossbank.payment.interceptor.PerformanceInterceptor
import com.tossbank.payment.interceptor.RateLimitInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    @Autowired
    private lateinit var performanceInterceptor: PerformanceInterceptor

    @Autowired
    private lateinit var rateLimitInterceptor: RateLimitInterceptor

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Rate Limiting을 먼저 적용 (context-path 제거로 /api 패턴 수정)
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/payments/**", "/accounts/**")
            .excludePathPatterns("/actuator/**", "/admin/**")

        // 성능 모니터링은 모든 요청에 적용
        registry.addInterceptor(performanceInterceptor)
            .addPathPatterns("/**")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 정적 리소스 캐시 비활성화 (개발용)
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(0)
    }
}
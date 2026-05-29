package com.tnear.adoptloop.admin.auth

import com.tnear.adoptloop.domain.repository.AdminRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

@Component
class AdminKeyFilter(
    private val adminRepository: AdminRepository,
    private val adminContext: AdminContext,
) : OncePerRequestFilter() {

    // context-path를 제거한 경로로 매칭 — requestURI 그대로 쓰면 context-path 설정 시 우회 위험,
    // servletPath는 MockMvc/일부 서블릿 매핑에서 비어 불안정.
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.removePrefix(request.contextPath).startsWith("/api/admin")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val raw = request.getHeader("X-Admin-Key")
        if (raw.isNullOrBlank()) { response.status = 401; return }

        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val admin = adminRepository.findByKeyHash(hash)
        if (admin == null) { response.status = 401; return }

        adminContext.adminId = admin.id
        chain.doFilter(request, response)
    }
}

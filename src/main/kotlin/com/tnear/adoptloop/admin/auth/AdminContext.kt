package com.tnear.adoptloop.admin.auth

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class AdminContext {
    var adminId: Long? = null
    fun require(): Long = adminId ?: error("admin not authenticated")
}

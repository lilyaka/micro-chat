package com.revotech.chatserver._config.multitenant.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.revotech.chatserver.helper.TokenHelper
import com.revotech.config.multitenant.TenantContext.currentTenant
import com.revotech.config.multitenant.filter.ATenantFilter
import com.revotech.exception.TenantException
import com.revotech.payload.ErrorPayload
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component("tenantFilter")
@Order(1)
class ChatFilter(private val tokenHelper: TokenHelper) : ATenantFilter() {
    @Value("\${tenants.tenant-header-key}")
    override var tenantHeaderKey: String = ""

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val token = (request as HttpServletRequest).getHeader("Authorization")
        if (token != null) {
            try {
                val tenant = tokenHelper.getTenantId(token.removePrefix("Bearer ")) ?: throw TenantException(
                    "tenantRequired",
                    "Tenant is required"
                )
                currentTenant = tenant
                chain.doFilter(request, response)
            } catch (e: TenantException) {
                val res = response as HttpServletResponse
                res.status = HttpStatus.BAD_REQUEST.value()
                res.contentType = MediaType.APPLICATION_JSON_VALUE
                res.writer.write(ObjectMapper().writeValueAsString(ErrorPayload(e.code, e.message)))
            } finally {
                currentTenant = null
            }
        } else {
            chain.doFilter(request, response)
        }
    }
}

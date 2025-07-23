package com.revotech.chatserver._config.multitenant.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.revotech.chatserver.helper.TokenHelper
import com.revotech.config.multitenant.TenantContext
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
import org.springframework.messaging.MessagingException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import java.io.IOException

@Component("tenantFilter")
@Order(1)
class ChatFilter(
    private val tokenHelper: TokenHelper,
    private val multipartResolver: StandardServletMultipartResolver = StandardServletMultipartResolver()
) : ATenantFilter() {

    @Value("\${tenants.tenant-header-key}")
    override var tenantHeaderKey: String = ""

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        try {
            val token = extractBearerToken(httpRequest)
            if (token != null) {
                processAuthenticatedRequest(token, httpRequest, httpResponse, chain)
            } else {
                chain.doFilter(request, response)
            }
        } catch (e: Exception) {
            logError("Unexpected error in ChatFilter", e)
            handleError(httpResponse, HttpStatus.INTERNAL_SERVER_ERROR, "filterError", "Request processing failed")
        }
    }

    private fun processAuthenticatedRequest(
        token: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            val claims = tokenHelper.getClaims(token)

            val tenant = claims["tenant"] as String? ?: throw TenantException(
                "tenantRequired",
                "Tenant claim not found in JWT"
            )
            currentTenant = tenant
            TenantContext.currentUser = claims.subject
            val authentication = tokenHelper.toPrincipal(token) as UsernamePasswordAuthenticationToken
            SecurityContextHolder.getContext().authentication = authentication
            if (isMultipartRequest(request)) {
                handleMultipartRequest(request, response, chain)
            } else {
                chain.doFilter(request, response)
            }
        } catch (e: TenantException) {
            logError("Tenant validation failed", e)
            handleError(response, HttpStatus.BAD_REQUEST, e.code, e.message)
        } catch (e: MessagingException) {
            logError("JWT validation failed", e)
            handleError(response, HttpStatus.UNAUTHORIZED, "invalidJwt", "Invalid or expired JWT token")
        } catch (e: io.jsonwebtoken.ExpiredJwtException) {
            logError("JWT expired", e)
            handleError(response, HttpStatus.UNAUTHORIZED, "expiredJwt", "JWT token has expired")
        } catch (e: io.jsonwebtoken.JwtException) {
            logError("JWT parsing failed", e)
            handleError(response, HttpStatus.UNAUTHORIZED, "invalidJwt", "Malformed JWT token")
        } catch (e: Exception) {
            logError("Authentication processing failed", e)
            handleError(response, HttpStatus.UNAUTHORIZED, "authError", "Authentication failed")
        } finally {
            cleanup()
        }
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.removePrefix("Bearer ").trim()
        } else null
    }

    private fun isMultipartRequest(request: HttpServletRequest): Boolean {
        val contentType = request.contentType
        return contentType != null && contentType.lowercase().startsWith("multipart/")
    }

    private fun handleMultipartRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            if (multipartResolver.isMultipart(request)) {
                chain.doFilter(request, response)
            } else {
                handleError(response, HttpStatus.BAD_REQUEST, "invalidMultipart", "Invalid multipart request format")
            }
        } catch (e: Exception) {
            logError("Failed to process multipart request", e)
            handleError(response, HttpStatus.BAD_REQUEST, "multipartError", "Failed to process multipart data")
        }
    }

    private fun cleanup() {
        try {
            currentTenant = null
            TenantContext.currentUser = null
            SecurityContextHolder.clearContext()
        } catch (e: Exception) {
            logError("Error during cleanup", e)
        }
    }

    private fun handleError(response: HttpServletResponse, status: HttpStatus, code: String, message: String) {
        try {
            if (!response.isCommitted) {
                response.status = status.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.characterEncoding = "UTF-8"

                val errorPayload = ErrorPayload(code, message)
                val jsonResponse = ObjectMapper().writeValueAsString(errorPayload)

                response.writer.write(jsonResponse)
                response.writer.flush()

                logError("Sent error response: $status - $code: $message", null)
            } else {
                logError("Response already committed, cannot send error: $code - $message", null)
            }
        } catch (e: IOException) {
            logError("Failed to write error response", e)
        }
    }

    private fun logError(message: String, exception: Exception?) {
        println("❌ ChatFilter: $message")
        exception?.let {
            println("❌ ChatFilter Exception: ${it.javaClass.simpleName}: ${it.message}")
            if (it.cause != null) {
                println("❌ ChatFilter Cause: ${it.cause?.javaClass?.simpleName}: ${it.cause?.message}")
            }
        }
    }
}
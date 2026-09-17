package com.blog.app.auth

import com.blog.app.data.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthUrlPolicyTest {
    @Test
    fun allowsOnlyTheConfiguredAuthorizationOrigin() {
        val policy = OAuthUrlPolicy("http://124.221.195.130/auth/")

        assertTrue(policy.isAllowed("http://124.221.195.130/auth/login"))
        assertTrue(policy.isAllowed("http://124.221.195.130/auth/oauth2/authorize?client_id=app"))
        assertFalse(policy.isAllowed("http://124.221.195.131/auth/login"))
        assertFalse(policy.isAllowed("http://124.221.195.130:8080/auth/login"))
        assertFalse(policy.isAllowed("https://124.221.195.130/auth/login"))
        assertFalse(policy.isAllowed("http://evil.example/auth/login"))
    }

    @Test
    fun authorizationRequestContainsState() {
        val request = AuthRepository().createAuthorizationRequest()

        assertNotNull(request.state)
        assertTrue(request.state!!.isNotBlank())
    }
}

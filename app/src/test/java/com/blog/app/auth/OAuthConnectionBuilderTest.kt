package com.blog.app.auth

import android.net.Uri
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection

class OAuthConnectionBuilderTest {
    private val builder = OAuthConnectionBuilder("http://124.221.195.130/auth/")

    @Test
    fun allowsConfiguredHttpOrigin() {
        val connection = builder.openConnection(
            Uri.parse("http://124.221.195.130/auth/oauth2/token")
        )

        assertTrue(connection is HttpURLConnection)
        connection.disconnect()
    }

    @Test
    fun rejectsAnotherHttpOrigin() {
        assertThrows(IllegalArgumentException::class.java) {
            builder.openConnection(Uri.parse("http://124.221.195.131/auth/oauth2/token"))
        }
    }

    @Test
    fun rejectsUnsupportedScheme() {
        assertThrows(java.io.IOException::class.java) {
            builder.openConnection(Uri.parse("ftp://124.221.195.130/auth/oauth2/token"))
        }
    }
}

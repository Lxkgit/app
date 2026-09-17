package com.blog.app.data.api

import com.blog.app.data.model.auth.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Blog OAuth2 authorization service API.
 */
interface AuthApi {
    /**
     * Logs in through the existing custom password grant.
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun login(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse
}

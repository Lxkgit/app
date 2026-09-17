package com.blog.app.data.api

import com.blog.app.data.model.auth.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 博客 OAuth2 授权服务接口。
 */
interface AuthApi {
    /**
     * 通过旧的自定义密码模式登录。
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

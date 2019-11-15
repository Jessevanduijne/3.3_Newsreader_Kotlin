package nl.vanduijne.jesse

import nl.vanduijne.jesse.model.*
import retrofit2.Call
import retrofit2.http.*

interface ArticleService {
    @GET("api/feeds")
    fun feeds(): Call<List<Category>>

    @GET("api/articles")
    fun articles(
        @Header("x-authtoken") authentication: String? = null,
        @Query("count") count: Int? = 20,
        @Query("feed") feedId: Int? = null
    ): Call<Articles>

    @GET("api/articles/{id}")
    fun articlesById(
        @Header("x-authtoken") authentication: String? = null,
        @Path("id") nextId: Int,
        @Query("count") count: Int? = 20,
        @Query("feed") feedId: Int? = null
    ): Call<Articles>


    @GET("api/articles/liked")
    fun getLikedArticles(
        @Header("x-authtoken") authentication: String,
        @Query("count") count: Int? = 20,
        @Query("feed") feedId: Int? = null
    ): Call<Articles>

    @PUT("api/articles/{id}//like")
    fun likeArticle (
        @Header("x-authtoken") authentication: String,
        @Path("id") articleId: Int
    ): Call<Void>

    @DELETE("api/articles/{id}//like")
    fun deleteLike(
        @Header("x-authtoken") authentication: String,
        @Path("id") articleId: Int
    ): Call<Void>


    @FormUrlEncoded
    @POST("api/users/register")
    fun register(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("api/users/login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>
}
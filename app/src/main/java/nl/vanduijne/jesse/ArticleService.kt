package nl.vanduijne.jesse

import nl.vanduijne.jesse.model.Article
import nl.vanduijne.jesse.model.Articles
import nl.vanduijne.jesse.model.Category
import nl.vanduijne.jesse.model.DefaultResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ArticleService {
    @GET("api/feeds")
    fun feeds(): Call<List<Category>>

    @GET("api/articles")
    fun articles(@Query("count") count: Int? = 20,
                 @Query("feed") feedId: Int? = null
    ): Call<Articles>

    @GET("api/articles/{id}")
    fun articlesById(@Path("id") articleId: Int,
                    @Query("count") count: Int? = 20,
                    @Query("feed") feedId: Int? = null
    ): Call<Articles>





    @Multipart
    @PUT("api/articles/{id}//like")
    fun addLike(
        @Part("article") article: RequestBody,
        @Part("like") like: RequestBody
    ): Call<Article>

    @DELETE("api/articles/{id}//like")
    fun deleteLike(
        @Part("article") article: RequestBody,
        @Part("like") like: RequestBody
    ): Call<Article>

    // TODO: Check if this is correct, powerpoint has multiple versions of writing a post
    @POST("api/users/register")
    fun register(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<DefaultResponse>

    @POST("api/users/login")
    fun login() // TODO: create call
}
package nl.vanduijne.jesse

import android.app.PendingIntent.getService
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import kotlinx.android.synthetic.main.activity_article_detail.*
import kotlinx.android.synthetic.main.articlelistitem.*
import kotlinx.android.synthetic.main.content_article_detail.*
import kotlinx.android.synthetic.main.content_main.*
import nl.vanduijne.jesse.model.Article
import nl.vanduijne.jesse.model.Articles
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.log

class ArticleDetail : AppCompatActivity() {

    private val service = getService()
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)
        setSupportActionBar(detailtoolbar)

        val toolbar = supportActionBar
        toolbar?.setDisplayHomeAsUpEnabled(true)

        detail_title.text = intent.getStringExtra("detail_title")
        detail_summary.text = intent.getStringExtra("detail_summary")
        detail_url.text = intent.getStringExtra("detail_url")
        detail_date.text = intent.getStringExtra("detail_date")

        val imageString = intent.getStringExtra("detail_image")
        val requestOption = RequestOptions().placeholder(R.drawable.placeholder).centerCrop() // Create placeholder
        Glide.with(this).load(imageString).apply(requestOption).into(detail_image)

        like_button.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        isLiked = intent.getBooleanExtra("detail_liked", false)
        setLikeButton()
    }

    private fun isLoggedIn(): Boolean {
        val authToken = getAuthToken()
        return authToken != ""
    }

    private fun setLikeButton(){
        if(isLoggedIn()) {
            if (isLiked) {
                like_button.setImageResource(R.drawable.ic_heart)
            } // default is already set in layout

            like_button.show()
            setLikeClickListener()
        }
        else like_button.hide()
    }

    private fun setLikeClickListener(){
        like_button.setOnClickListener {
            val authToken = getAuthToken()
            val articleId = intent.getIntExtra("detail_id", 0)

            if(isLiked) {
                val call = service.deleteLike(authToken, articleId)
                call.enqueue(object: Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if(response.isSuccessful) {
                            Log.e("HTTP", "Successfully removed a like from an article")
                            like_button.setImageResource(R.drawable.ic_emptyheart)
                            isLiked = false
                        }
                        else Log.e("HTTP", "Removing a like failed")
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("HTTP", "Removing like from article failed")
                    }
                })
            }
            else {
                val call = service.likeArticle(authToken, articleId)
                call.enqueue(object: Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if(response.isSuccessful) {
                            Log.e("HTTP", "Successfully liked an article")
                            like_button.setImageResource(R.drawable.ic_heart)
                            isLiked = true
                        }
                        else Log.e("HTTP", "Placing like: empty response")
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("HTTP", "Placing like on article failed")
                    }
                })
            }
        }
    }

    private fun getAuthToken(): String {
        val sharedPrefs = getSharedPreferences("nl.vanduijne.jesse", Context.MODE_PRIVATE)
        val authTokenKey = getString(R.string.authTokenKey)
        val authToken = sharedPrefs.getString(authTokenKey, "")
        return authToken!!
    }

    private fun getService(): ArticleService {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://inhollandbackend.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ArticleService::class.java)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

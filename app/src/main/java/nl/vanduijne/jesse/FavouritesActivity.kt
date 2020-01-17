package nl.vanduijne.jesse

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_main.*
import nl.vanduijne.jesse.helpers.getService
import nl.vanduijne.jesse.helpers.hideSpinner
import nl.vanduijne.jesse.helpers.showSpinner
import nl.vanduijne.jesse.model.Article
import nl.vanduijne.jesse.model.Articles
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavouritesActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager

    private val articles = ArrayList<Article>()
    private val service = getService()
    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager.findLastVisibleItemPosition()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_main)

        // Back button:
//        val toolbar = supportActionBar
      //  toolbar?.setDisplayHomeAsUpEnabled(true)

        showSpinner(mainscreen)
        linearLayoutManager = LinearLayoutManager(this)
        getArticles()
        getScrollListener()
        setOnRefreshListener()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setOnRefreshListener(){
        swipe_refresh_articles.setOnRefreshListener {
            hideSpinner(mainscreen)
            articles.clear()
            getArticles()
        }
    }

    private fun getClickListener() : ArticleClickListener {
        return object: ArticleClickListener {
            override fun onItemClick(position: Int) {
                createInterface(position, articles)
            }
        }
    }

    private fun createInterface(position: Int, articles: ArrayList<Article>) {
        val intent = Intent(this@FavouritesActivity, ArticleDetail::class.java)
        val item = articles[position]

        intent.putExtra("detail_title", item.Title)
        intent.putExtra("detail_summary", item.Summary)
        intent.putExtra("detail_image", item.Image)
        intent.putExtra("detail_url", item.Url)
        intent.putExtra("detail_date", item.PublishDate)
        intent.putExtra("detail_id", item.Id)
        intent.putExtra("detail_liked", item.IsLiked)
        startActivity(intent)
    }

    private fun getNewFavouriteArticles(nextId: Int, feedId: Int? = 0){
        showSpinner(mainscreen)
        val call: Call<Articles>
        if(feedId != 0) {
            call = service.getLikedArticles(feedId = feedId, authentication = getAuthToken()!!)
        }
        else  call = service.articlesById(nextId = nextId, authentication = getAuthToken())

        call.enqueue(object : Callback<Articles> {
            override fun onResponse(call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    articles.addAll(result!!)
                    recyclerview.adapter!!.notifyDataSetChanged()
                }
                else Log.e("HTTP Response", "Response is unsuccessful of empty")
                hideSpinner(mainscreen)
            }
            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data while trying to load more articles")
                hideSpinner(mainscreen)
            }
        })
    }

    private fun getArticles(){
        val authToken = getAuthToken()
        val call: Call<Articles> = service.getLikedArticles(authToken!!)

        call.enqueue(object: Callback<Articles> {
            override fun onResponse(call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    result?.toCollection(articles) // Make articles accessible for the recyclerview
                    recyclerview.adapter!!.notifyDataSetChanged()
                }
                else Log.e("HTTP Response", "Response is unsuccessful of empty")
                hideSpinner(mainscreen)
                swipe_refresh_articles.isRefreshing = false
                setMessage()
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data: ")
                hideSpinner(mainscreen)
                swipe_refresh_articles.isRefreshing = false
            }
        })

        // After articles are loaded, make them clickable:
        val articleClickListener = getClickListener()
        recyclerview.layoutManager = linearLayoutManager
        recyclerview.adapter = ListAdapter(this, articles, articleClickListener)
    }


    private fun getScrollListener(){
        recyclerview.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val totalItemCount = recyclerView.layoutManager!!.itemCount // recyclerView.adapter!!.getItemCount()
                if(totalItemCount == lastVisibleItemPosition + 1 ) { // Plus one cause count = 20 and position is 19 (starts at 0)
                    println("hitting the onscroll")
                    val nextId = articles[lastVisibleItemPosition].Id - 1
                    //getNewArticles(nextId = nextId)
                }
            }
        })
    }


    private fun setMessage(){
        if(articles.count() == 0) {
            favourites_list_empty.visibility = View.VISIBLE
        }
    }

    private fun getAuthToken(): String? {
        val sharedPrefs = getSharedPreferences("nl.vanduijne.jesse", Context.MODE_PRIVATE)
        val authTokenKey = getString(R.string.authTokenKey)
        val authToken = sharedPrefs.getString(authTokenKey, "")
        return if(authToken != "") authToken
        else authToken
    }
}

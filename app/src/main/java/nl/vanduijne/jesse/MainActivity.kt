package nl.vanduijne.jesse

import android.app.Activity
import android.app.PendingIntent.getService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import nl.vanduijne.jesse.model.Articles
import nl.vanduijne.jesse.model.Article
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager.findLastVisibleItemPosition()
    private val articles = ArrayList<Article>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        linearLayoutManager = LinearLayoutManager(this)

        getArticles()
        getScrollListener()
    }

    private fun getClickListener() : ArticleClickListener {
        return object: ArticleClickListener {
            override fun onItemClick(position: Int) {
                createInterface(position, articles)
            }
        }
    }

    private fun getScrollListener(){
        recyclerview.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val totalItemCount = recyclerView.layoutManager!!.itemCount // recyclerView.adapter!!.getItemCount()
                if(totalItemCount == lastVisibleItemPosition + 1 ) { // Plus one cause count = 20 and position is 19 (starts at 0)

                    val nextId = articles[lastVisibleItemPosition].Id - 1 // TODO: Get nextId from Articles class instead of Article
                    getNewArticles(nextId)
                    loadingPanel.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun getNewArticles(nextId: Int){
        val service = getService()
        val call = service.article(nextId)
        call.enqueue(object : Callback<Articles> {
            override fun onResponse(call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    articles.addAll(result!!)
                    recyclerview.adapter!!.notifyDataSetChanged()
                    loadingPanel.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data while trying to load more articles")
            }
        })
    }

    private fun getArticles(){
        val service = getService()
        val call = service.articles()

        call.enqueue(object: Callback<Articles> {
            override fun onResponse( call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    result?.toCollection(articles) // Make articles accessible for the recyclerview
                    recyclerview.adapter!!.notifyDataSetChanged()
                    loadingPanel.visibility = View.GONE
                }
                else Log.e("HTTP Response", "Response is unsuccessful of empty")
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data")
            }
        })


        val articleClickListener = getClickListener()
        recyclerview.layoutManager = linearLayoutManager
        recyclerview.adapter = ListAdapter(this, articles, articleClickListener)
    }

    private fun getService(): ArticleService {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://inhollandbackend.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ArticleService::class.java)
        return service
    }

    private fun createInterface(position: Int, articles: ArrayList<Article>) {
        val intent = Intent(this@MainActivity, ArticleDetail::class.java)
        val item = articles[position]

        intent.putExtra("detailtitle", item.Title)
        intent.putExtra("detailsummary", item.Summary)
        intent.putExtra("detailimage", item.Image)
        intent.putExtra("detailurl", item.Url)
        intent.putExtra("detaildate", item.PublishDate)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}

package nl.vanduijne.jesse

import android.app.PendingIntent.getService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import nl.vanduijne.jesse.model.Articles
import nl.vanduijne.jesse.model.Article
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var drawer: DrawerLayout

    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager.findLastVisibleItemPosition()
    private val articles = ArrayList<Article>()
    private val service = getService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Add drawer toggle
        getNavigationDrawer()

        linearLayoutManager = LinearLayoutManager(this)
        getArticles()
        getScrollListener()

        // Test 3
    }

    private fun getNavigationDrawer(){
        setDrawerLoginState() // show logout button, favs etc.

        drawer = drawer_layout
        val toggleableDrawer = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggleableDrawer)
        toggleableDrawer.syncState()

        // Add button functionality for drawer
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun setDrawerLoginState(){
        val favourites = nav_view.menu.findItem(R.id.favourites)
        val logout = nav_view.menu.findItem(R.id.logout)
        val login = nav_view.menu.findItem(R.id.login)

        if(isLoggedIn()) {
            logout.setVisible(true)
            favourites.setVisible(true)
            login.setVisible(false)
        }
        else {
            logout.setVisible(false)
            favourites.setVisible(false)
            login.setVisible(true)
        }
    }

    private fun getAuthToken(): String? {
        val sharedPrefs = getSharedPreferences("nl.vanduijne.jesse", Context.MODE_PRIVATE)
        val authTokenKey = getString(R.string.authTokenKey)
        val authToken = sharedPrefs.getString(authTokenKey, "")
        return if(authToken != "") authToken
        else authToken
    }

    private fun isLoggedIn(): Boolean {
        val authToken = getAuthToken()
        return authToken != null && authToken != ""
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
                    println("hitting the onscroll")
                    val nextId = articles[lastVisibleItemPosition].Id - 1
                    getNewArticles(nextId = nextId)
                }
            }
        })
    }

    private fun getNewArticles(nextId: Int, feedId: Int? = 0){
        val call: Call<Articles>
        if(feedId != 0)
            if(isLoggedIn())
                call = service.articlesById(nextId = nextId, feedId = feedId, authentication = getAuthToken())
            else call = service.articlesById(nextId = nextId, feedId = feedId)
        else
            if(isLoggedIn())
                call = service.articlesById(nextId = nextId, authentication = getAuthToken())
            else call = service.articlesById(nextId = nextId)

        call.enqueue(object : Callback<Articles> {
            override fun onResponse(call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    articles.addAll(result!!)
                    recyclerview.adapter!!.notifyDataSetChanged()
                }
                else Log.e("HTTP Response", "Response is unsuccessful of empty")
            }
            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data while trying to load more articles")
            }
        })
    }

    private fun getArticles(){
        val call: Call<Articles>
        if(isLoggedIn())
            call = service.articles(authentication = getAuthToken())
        else
            call = service.articles()

        call.enqueue(object: Callback<Articles> {
            override fun onResponse( call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    result?.toCollection(articles) // Make articles accessible for the recyclerview
                    recyclerview.adapter!!.notifyDataSetChanged()
                }
                else Log.e("HTTP Response", "Response is unsuccessful of empty")
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data: ")
            }
        })

        // After articles are loaded, make them clickable:
        val articleClickListener = getClickListener()
        recyclerview.layoutManager = linearLayoutManager
        recyclerview.adapter = ListAdapter(this, articles, articleClickListener)
    }

    private fun getService(): ArticleService {
        val okHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://inhollandbackend.azurewebsites.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ArticleService::class.java)
        return service
    }

    private fun logout() {
        val sharedPrefs = getSharedPreferences("nl.vanduijne.jesse", Context.MODE_PRIVATE)
        val authTokenKey = getString(R.string.authTokenKey)
        sharedPrefs.edit().remove(authTokenKey).apply()
        setDrawerLoginState() // Add login, remove favourites & logout buttons

        finish()
        startActivity(getIntent())
    }

    private fun createInterface(position: Int, articles: ArrayList<Article>) {
        val intent = Intent(this@MainActivity, ArticleDetail::class.java)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.login -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            R.id.logout -> logout()
            R.id.cat_algemeen -> Toast.makeText(this, "algemeen", Toast.LENGTH_SHORT).show()
            else -> {
                return false
            }
        }

        drawer.closeDrawer(GravityCompat.START)

        // Item will be selected after action is triggered --> always true
        return true
    }

    override fun onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        }
        else {
            super.onBackPressed()
        }
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

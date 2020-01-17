package nl.vanduijne.jesse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import nl.vanduijne.jesse.helpers.hideSpinner
import nl.vanduijne.jesse.helpers.showSpinner
import nl.vanduijne.jesse.model.Articles
import nl.vanduijne.jesse.model.Article
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var drawer: DrawerLayout

    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager.findLastVisibleItemPosition()
    private val articles = ArrayList<Article>()
    private val service = nl.vanduijne.jesse.helpers.getService()

    private var isFavourites = false
    private var activeCall = false
    private var currentCategory = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Add drawer toggle
        showSpinner(mainscreen)
        getNavigationDrawer()
        linearLayoutManager = LinearLayoutManager(this)
        getArticles()
        getScrollListener()
        setOnRefreshListener()
    }

    private fun setOnRefreshListener(){
        swipe_refresh_articles.setOnRefreshListener {
            if(!activeCall) {
                hideSpinner(mainscreen)
                articles.clear()
                if(!isFavourites) {
                    getArticles()
                }
                else getFavouriteArticles()
            }
        }
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
                if(!isFavourites && !activeCall) {
                    val totalItemCount = recyclerView.layoutManager!!.itemCount
                    if(totalItemCount == lastVisibleItemPosition + 1 ) {
                        val nextId = articles[lastVisibleItemPosition].Id - 1
                        activeCall = true
                        getNewArticles(nextId = nextId)
                    }
                }
            }
        })
    }

    private fun getNewArticles(nextId: Int){
        val feedId = currentCategory
        showSpinner(mainscreen)
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
                else Log.e(getString(R.string.log_http_tag), getString(R.string.log_unsuccesful))
                hideSpinner(mainscreen)
                activeCall = false
            }
            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e(getString(R.string.log_http_tag), getString(R.string.log_no_data_articles))
                hideSpinner(mainscreen)
                activeCall = false
            }
        })
    }

    private fun getArticles(){
        val feedId = currentCategory
        val call: Call<Articles>
        if(isLoggedIn())
            call = service.articles(authentication = getAuthToken(), feedId = feedId)
        else
            call = service.articles(feedId = feedId)

        call.enqueue(object: Callback<Articles> {
            override fun onResponse( call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    result?.toCollection(articles) // Make articles accessible for the recyclerview
                    recyclerview.adapter!!.notifyDataSetChanged()
                }
                else Log.e(getString(R.string.log_http_tag), getString(R.string.log_no_data))
                hideSpinner(mainscreen)
                swipe_refresh_articles.isRefreshing = false
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e(getString(R.string.log_http_tag), getString(R.string.log_no_data))
                hideSpinner(mainscreen)
                swipe_refresh_articles.isRefreshing = false
            }
        })

        // After articles are loaded, make them clickable:
        val articleClickListener = getClickListener()
        recyclerview.layoutManager = linearLayoutManager
        recyclerview.adapter = ListAdapter(this, articles, articleClickListener)
    }

    private fun getFavouriteArticles(){
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
                else Log.e(getString(R.string.log_http_tag), getString(R.string.log_response_unsuccessful))
                hideSpinner(mainscreen)
                swipe_refresh_articles.isRefreshing = false
                showMessage()
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e(getString(R.string.log_http_tag), getString(R.string.log_no_data))
                hideSpinner(mainscreen)
                swipe_refresh_articles.isRefreshing = false
            }
        })

        // After articles are loaded, make them clickable:
        val articleClickListener = getClickListener()
        recyclerview.layoutManager = linearLayoutManager
        recyclerview.adapter = ListAdapter(this, articles, articleClickListener)
    }

    private fun showMessage(){
        if(articles.count() == 0) {
            favourites_list_empty.visibility = View.VISIBLE
            recyclerview.visibility = View.GONE
        }
    }

    private fun hideMessage(){
        recyclerview.visibility = View.VISIBLE
        favourites_list_empty.visibility = View.GONE
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
        hideMessage()
        when(item.itemId) {
            R.id.login -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            R.id.logout -> logout()
            R.id.all_articles -> {
                isFavourites = false
                showSpinner(mainscreen)
                articles.clear()
                getArticles()
            }
            R.id.favourites -> {
                isFavourites = true
                showSpinner(mainscreen)
                articles.clear()
                getFavouriteArticles()
            }
            R.id.cat_algemeen -> {
                showSpinner(mainscreen)
                articles.clear()
                currentCategory = 1
                getArticles()
            }
            R.id.cat_internet -> {
                showSpinner(mainscreen)
                articles.clear()
                currentCategory = 2
                getArticles()
            }
            R.id.cat_sport -> {
                showSpinner(mainscreen)
                articles.clear()
                currentCategory = 3
                getArticles()
            }
            R.id.cat_opmerkelijk -> {
                showSpinner(mainscreen)
                articles.clear()
                currentCategory = 4
                getArticles()
            }
            R.id.cat_games -> {
                showSpinner(mainscreen)
                articles.clear()
                currentCategory = 5
                getArticles()
            }
            R.id.cat_wetenschap -> {
                showSpinner(mainscreen)
                articles.clear()
                currentCategory = 6
                getArticles()
            }
            else -> {
                return false
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true // Item will be selected after action is triggered --> always true
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
}

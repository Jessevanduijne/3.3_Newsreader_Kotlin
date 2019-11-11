package nl.vanduijne.jesse

import android.app.PendingIntent.getService
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
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_register.*
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

        // Link content to this class
        setContentView(R.layout.activity_main)

        // Add toolbar
        setSupportActionBar(toolbar)

        // Add drawer toggle
        drawer = drawer_layout
        val toggleableDrawer = ActionBarDrawerToggle(this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggleableDrawer)
        toggleableDrawer.syncState()

        // Add button functionality for drawer
        var navview = nav_view
        navview.setNavigationItemSelectedListener(this)

        //if(savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, LoginFragment())
            navview.setCheckedItem(R.id.login)
        //}


        linearLayoutManager = LinearLayoutManager(this)

        getArticles()
        getScrollListener()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.login -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, LoginFragment())
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
                    getNewArticles(nextId)
                }
            }
        })
    }

    private fun getNewArticles(nextId: Int){

        val call = service.article(nextId)
        call.enqueue(object : Callback<Articles> {
            override fun onResponse(call: Call<Articles>, response: Response<Articles>) {
                if(response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val result = body?.Results
                    articles.addAll(result!!)
                    recyclerview.adapter!!.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<Articles>, t: Throwable) {
                Log.e("HTTP", "Couldn't fetch any data while trying to load more articles")
            }
        })
    }

    private fun getArticles(nextId: Int? = 0, feedId: Int? = 0){

        val call : Call<Articles>
        if(nextId == 0) {
            call = service.articles(20, null)
        }
        else {
            call = service.articlesById(nextId!!.toInt(), 20, feedId)
        }


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

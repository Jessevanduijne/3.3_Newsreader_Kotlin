package nl.vanduijne.jesse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import kotlinx.android.synthetic.main.activity_article_detail.*
import kotlinx.android.synthetic.main.content_article_detail.*

class ArticleDetail : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)
        setSupportActionBar(detailtoolbar)

        var toolbar = supportActionBar
        toolbar?.setDisplayHomeAsUpEnabled(true)

        detailtitle.text = intent.getStringExtra("detailtitle")
        detailsummary.text = intent.getStringExtra("detailsummary")
        detailurl.text = intent.getStringExtra("detailurl")
        detaildate.text = intent.getStringExtra("detaildate")

        val imageString = intent.getStringExtra("detailimage")
        val requestOption = RequestOptions().placeholder(R.drawable.placeholder).centerCrop() // Create placeholder
        Glide.with(this).load(imageString).apply(requestOption).into(detailimage)

        like.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
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

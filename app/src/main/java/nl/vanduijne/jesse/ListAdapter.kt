package nl.vanduijne.jesse

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.articlelistitem.view.*
import nl.vanduijne.jesse.model.Article

class ListAdapter (val context: Context, val items: ArrayList<Article>, val clickListener: ArticleClickListener): RecyclerView.Adapter<ListAdapter.ViewHolder>(){

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.listitemtitle
        val image: ImageView = itemView.listitemimage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.articlelistitem, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position] // list.get(position)
        holder.title.text = item.Title

        val requestOption =
            RequestOptions().placeholder(R.drawable.placeholder).centerCrop() // Create placeholder
        Glide.with(context).load(item.Image).apply(requestOption).into(holder.image)

        holder.itemView.setOnClickListener {
            clickListener.onItemClick(holder.adapterPosition)
        }

    }



}
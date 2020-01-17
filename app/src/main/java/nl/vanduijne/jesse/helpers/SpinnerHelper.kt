package nl.vanduijne.jesse.helpers

import android.view.View
import android.widget.ProgressBar
import nl.vanduijne.jesse.R

fun showSpinner(view: View){
    val loadingSpinner: ProgressBar = view.findViewById(R.id.loadingSpinner)
    loadingSpinner.bringToFront()
    loadingSpinner.visibility = View.VISIBLE
}

fun hideSpinner(view: View) {
    val loadingSpinner: ProgressBar = view.findViewById(R.id.loadingSpinner)
    loadingSpinner.visibility = View.GONE
}
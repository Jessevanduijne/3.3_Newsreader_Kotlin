package nl.vanduijne.jesse.model

data class Article ( // was: Results

    val Id : Int,
    val Feed : Int,
    val Title : String,
    val Summary : String,
    val PublishDate : String,
    val Image : String,
    val Url : String,
    val Related : List<String>,
    val Categories : List<Category>,
    val IsLiked : Boolean
)
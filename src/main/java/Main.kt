import com.squareup.moshi.*
import okio.Buffer
import twitter4j.TwitterFactory
import java.io.File
import java.io.IOException

val isCi = (getEnv("CI") == "true")

fun main() {

    getTwitterFactory()

    val twitter = TwitterFactory.getSingleton()

    val hololiveNames = members
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { it.split("\\s+".toRegex())[0] }
        .toList()

    val hololiveIds = members
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { it.split("\\s+".toRegex())[1] }
        .toList()

    val hololiveGenerations = members
        .lineSequence()
        .filter { it.isNotBlank() }
        .map {
            val generationString = it.split("\\s+".toRegex())[2]
            if (generationString.contains(",")) {
                return@map generationString.split(",")
            } else {
                return@map mutableListOf(generationString)
            }
        }
        .toList()


    val hololiveMenbers = twitter.lookupUsers(*hololiveIds.toTypedArray())

    for ((index, user) in hololiveMenbers.withIndex()) {
        println((index + 1).toString() + "," + hololiveNames[index] + "," + user.profileImageURLHttps.replaceBiggerSizeUrl())
    }

    val hololiveMemberList = mutableListOf<HololiveMember>()
    for ((index, user) in hololiveMenbers.withIndex()) {

        val hololiveMember = HololiveMember(
            id = (index + 1),
            name = hololiveNames[index],
            imageUrl = user.profileImageURL.replaceBiggerSizeUrl(),
            generation = hololiveGenerations[index]
        )

        hololiveMemberList.add(hololiveMember)
    }

    val type = Types.newParameterizedType(List::class.java, HololiveMember::class.java)

    val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build().adapter<List<HololiveMember>>(type)

//    println(adapter.toJson(hololiveMemberList))

    print(toPretty(adapter, hololiveMemberList))


    File("public/members.json").writeText(toPretty(adapter, hololiveMemberList))

}

fun getEnv(key: String): String {
    return System.getenv(key) ?: ""
}

fun getTwitterFactory() {

    if (isCi) {
        val consumerKey = getEnv("TWITTER4J.OAUTH.CONSUMERKEY")
        val consumerSecret = getEnv("twitter4j.oauth.consumerSecret")
        val accessToken = getEnv("twitter4j.oauth.accessToken")
        val accessTokenSecret = getEnv("twitter4j.oauth.accessTokenSecret")

        if (consumerKey.isBlank()) {

            println("consumerKey is blank")
            throw IllegalStateException("consumerKey is blank")
        }
    }
}

private fun String.replaceBiggerSizeUrl(): String {
    return this.replace("normal", "400x400")
}

private fun toPretty(adapter: JsonAdapter<List<HololiveMember>>, member: List<HololiveMember>): String {
    val buffer = Buffer()

    val prettyPrintWriter = JsonWriter.of(buffer)
    prettyPrintWriter.indent = "  "

    try {
        adapter.toJson(prettyPrintWriter, member)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return buffer.readUtf8()
}
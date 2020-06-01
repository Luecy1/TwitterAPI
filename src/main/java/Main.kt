import com.squareup.moshi.*
import okio.Buffer
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.io.File
import java.io.IOException

val isCi = (getEnv("CI") == "true")

fun main() {

    getTwitterFactory()

    val twitter = getTwitterFactory()

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

fun getTwitterFactory(): Twitter {

    if (isCi) {

        val consumerKey = getEnv("OAUTH_CONSUMERKEY")
        val consumerSecret = getEnv("OAUTH_CONSUMERSECRET")
        val accessToken = getEnv("OAUTH_ACCESSTOKEN")
        val accessTokenSecret = getEnv("OAUTH_ACCESSTOKENSECRET")

        val configuration = ConfigurationBuilder()
            .setOAuthConsumerKey(consumerKey)
            .setOAuthConsumerSecret(consumerSecret)
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret)
            .build()

        return TwitterFactory(configuration).instance
    } else {
        return TwitterFactory.getSingleton()
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
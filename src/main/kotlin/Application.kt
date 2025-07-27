package site.remlit.blueb

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.nio.file.Files
import java.security.SecureRandom
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

val tokenFilePath = Path("./token.txt")
val dokkaDirectoryPath = Path("./dokka")
val javadocDirectoryPath = Path("./javadoc")

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    Files.createDirectories(dokkaDirectoryPath)
    Files.createDirectories(javadocDirectoryPath)

    if (!Files.exists(tokenFilePath)) {
        tokenFilePath.createFile()

        val random = SecureRandom()

        val bytes = ByteArray(16)
        random.nextBytes(bytes)

        val string = BigInteger(1, bytes)
            .toString(32)
            .padStart(16, '0')

        log.info("Token generated, see token.txt")

        tokenFilePath.writeText(string)
    }

    configureRouting()
}

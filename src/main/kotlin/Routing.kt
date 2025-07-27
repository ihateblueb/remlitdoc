package site.remlit.blueb

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.streams.toList

fun Application.configureRouting() {
    routing {
        get("/") {
            val dokka = try { Files.list(Path("./dokka")).toList().filter { it.isDirectory() } } catch (e: Exception) { emptyList() }
            val javadoc = try { Files.list(Path("./javadoc")).toList().filter { it.isDirectory() } } catch (e: Exception) { emptyList() }

            call.respondHtml {
                head {
                    title { +"Documentation" }
                    link { rel = "preconnect"; href = "https://fonts.googleapis.com" }
                    link { rel = "preconnect"; href = "https://fonts.gstatic.com" }
                    link { rel = "stylesheet"; href = "https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap" }
                    link { rel = "stylesheet"; href = "/resources/index.css" }
                }
                body {
                    h1 { +"Documentation" }
                    p { +"This contains the documentation of various projects of mine. Feel free to browse around." }

                    fun generateLists(paths: List<Path>, platform: String) {
                        ul {
                            for (entry in paths) {
                                li {
                                    span { + entry.name }
                                    ul {
                                        for (version in entry.toFile().listFiles()) {
                                            li {
                                                a {
                                                    href = "$platform/${entry.name}/${version.name}/"
                                                    + version.name
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div {
                        classes = setOf("platformHeader")
                        img { src = "/resources/kotlin.png" }
                        h2 { +"Dokka" }
                    }
                    generateLists(dokka, "dokka")
                    div {
                        classes = setOf("platformHeader")
                        img { src = "/resources/java.png" }
                        h2 { +"Javadoc" }
                    }
                    generateLists(javadoc, "javadoc")
                }
            }
        }

        get("/{platform}/{project}/{version}/{docPath...}") {
            val platform = call.parameters["platform"]
            val project = call.parameters["project"]
            val version = call.parameters["version"]

            if (platform.isNullOrEmpty() || !Files.exists(Path("./$platform"))) {
                call.respondHtml(HttpStatusCode.NotFound) {
                    head { title { +"Not Found" } }
                    body {
                        p { +"Platform $platform not found" }
                    }
                }
            }

            if (project.isNullOrEmpty() || !Files.exists(Path("./$platform/$project"))) {
                call.respondHtml(HttpStatusCode.NotFound) {
                    head { title { +"Not Found" } }
                    body {
                        p { +"Project $project not found" }
                    }
                }
            }

            if (version.isNullOrEmpty() || !Files.exists(Path("./$platform/$project/$version"))) {
                call.respondHtml(HttpStatusCode.NotFound) {
                    head { title { +"Not Found" } }
                    body {
                        p { +"Version $version not found" }
                    }
                }
            }

            val zip = Path("./$platform/$project/$version/$project-$version-$platform.zip")
            if (!zip.exists()) {
                call.respondHtml(HttpStatusCode.NotFound) {
                    head { title { +"Not Found" } }
                    body {
                        p { +"Archive ${zip.pathString} not found" }
                    }
                }
            }

            var pathInZip = call.parameters.getAll("docPath")?.joinToString("/") ?: "index.html"
            if (pathInZip.isBlank()) pathInZip = "index.html"

            try {
                ZipFile(zip.toFile()).use { archive ->
                    val entry = (archive.getEntry(pathInZip))!!
                    val inputStream = archive.getInputStream(entry)
                    val contentType = when (entry.name.substringAfterLast('.')) {
                        "html" -> ContentType.Text.Html
                        "css", "min.css" -> ContentType.Text.CSS
                        "js" -> ContentType.Text.JavaScript
                        "svg" -> ContentType.Image.SVG
                        "png" -> ContentType.Image.PNG
                        "jpg", "jpeg" -> ContentType.Image.JPEG
                        "json" -> ContentType.Application.Json
                        else -> ContentType.Application.OctetStream
                    }
                    call.respondBytes(inputStream.readBytes(), contentType)
                }
            } catch (e: Exception) {
                call.respondHtml(HttpStatusCode.NotFound) {
                    head { title { +"Error" } }
                    body {
                        p { +"Something went wrong opening this archive." }
                    }
                }
            }
        }

        staticResources("/resources", "static")

        post("/publish") {
            val body = call.receive<PublishBody>()
            val token = Files.readString(Path("./token.txt"))

            if (body.token != token) {
                call.respond(HttpStatusCode.Unauthorized, "Token invalid")
                return@post
            }

            log.info("Downloading ${body.provider}/${body.name}/${body.version} from ${body.source}...")

            Files.createDirectories(Path("./dokka/${body.name}/${body.version}"))

            val archiveName = body.source.substringAfterLast('/')
            val archivePath = Path("./dokka/${body.name}/${body.version}/$archiveName")
            if (!archivePath.exists()) Files.createFile(archivePath)

            try {
                val input = URL(body.source).openStream()
                Files.copy(input, archivePath, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {}

            log.info("Completed.")

            call.respondText("$body")
        }

        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")
    }
}

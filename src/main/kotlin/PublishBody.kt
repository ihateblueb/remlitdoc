package site.remlit.blueb

import kotlinx.serialization.Serializable

@Serializable
data class PublishBody(
    val token: String,

    // dokka, javadoc
    val provider: String,
    // http-signature-utility
    val name: String,
    // 2025.7.2.9
    val version: String,

    // https://jenkins.remlit.site/job/HttpSignatureUtility/7/artifact/build/distributions/http-signature-utility-2025.7.2.9-SNAPSHOT-dokka.zip
    val source: String,
)

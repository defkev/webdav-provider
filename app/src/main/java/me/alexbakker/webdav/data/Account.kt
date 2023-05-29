package me.alexbakker.webdav.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.security.KeyChain
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import me.alexbakker.webdav.BuildConfig
import me.alexbakker.webdav.WebDavApplication
import me.alexbakker.webdav.provider.WebDavClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.nio.file.Path
import java.nio.file.Paths

@Entity(tableName = "account")
data class Account(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "name")
    var name: String? = null,

    @ColumnInfo(name = "url")
    var url: String? = null,

    @ColumnInfo(name = "protocol", defaultValue = "AUTO")
    var protocol: Protocol = Protocol.AUTO,

    @ColumnInfo(name = "verify_certs")
    var verifyCerts: Boolean = true,

    @ColumnInfo(name = "username")
    var username: String? = null,

    @ColumnInfo(name = "password")
    var password: String? = null,

    @ColumnInfo(name = "client_certificate")
    var clientCertificate: String? = null,

    @ColumnInfo(name = "max_cache_file_size")
    var maxCacheFileSize: Long = 20
) {
    private val authentication: Boolean
        get() {
            return username != null && password != null
        }

    private val mutual_authentication: Boolean
        get() {
            return !clientCertificate.isNullOrBlank()
        }

    val rootPath: Path
        get() {
            val path = ensureTrailingSlash(baseUrl.encodedPath)
            return Paths.get(path)
        }

    val rootId: String
        get() {
            return id.toString()
        }

    val rootUri: Uri
        get() {
            return DocumentsContract.buildRootUri(BuildConfig.PROVIDER_AUTHORITY, rootId)
        }

    @Ignore
    private var _client: WebDavClient? = null

    private val baseUrl: HttpUrl
        get() {
            return ensureTrailingSlash(url!!).toHttpUrl()
        }

    val client: WebDavClient
        get() {
            if (_client == null) {
                val creds = if (authentication) Pair(username!!, password!!) else null
                val mutualCreds = if (mutual_authentication) {
                    val context: Context = WebDavApplication.applicationContext()
                    try {
                        Triple(
                            clientCertificate!!,
                            KeyChain.getPrivateKey(context, clientCertificate!!)!!,
                            KeyChain.getCertificateChain(context, clientCertificate!!)!!
                        )
                    } catch (e: NullPointerException) {
                        // TODO: maybe add some UI element in case the user removes the key from the KeyStore without updating the account?
                        null
                    }
                } else {
                    null
                }
                _client = WebDavClient(baseUrl, creds, mutualCreds, verifyCerts, noHttp2 = protocol != Protocol.AUTO)
            }

            return _client!!
        }

    fun resetState() {
        _client = null
    }

    private fun ensureTrailingSlash(s: String): String {
        return if (!s.endsWith("/")) {
            "$s/"
        } else {
            s
        }
    }

    enum class Protocol {
        AUTO, HTTP1
    }
}

fun List<Account>.byId(id: Long): Account {
    return this.single { v -> v.id == id }
}

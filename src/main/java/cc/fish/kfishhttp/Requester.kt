package cc.fish.kfishhttp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import cc.fish.kfishhttp.extensions.ThreadPool
import cc.fish.kfishhttp.net.NetCore
import cc.fish.kfishhttp.net.NetUtils
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URLEncoder
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/**
 * Created by fish on 18-2-27.
 */
class Requester<Result>() {
    var mUrl = StringBuilder()
    var mBody = StringBuilder()
    var mHeader = mutableMapOf<String, String>()
    var mIsJsonBody = false
    var mSSLContext: SSLContext? = null
    var mType: Class<Result>? = null

    constructor(parcel: Parcel) : this() {
        mIsJsonBody = parcel.readByte() != 0.toByte()
    }

    fun url(url: String) = mUrl.run {
        delete(0, length)
        append(url)
    }

    fun urlParam(key: String, value: Any) {
        if (mUrl.isNotEmpty())
            mUrl.append("?$key=${URLEncoder.encode(value.toString(), "UTF-8")}")
    }

    fun urlParam(params: Array<Pair<String, Any>>) {
        if (mUrl.isNotEmpty())
            params.map { urlParam(it.first, it.second) }
    }

    fun jsonBody(json: String) {
        mIsJsonBody = true
        mBody.run {
            delete(0, length)
            append(json)
        }
    }

    fun body(key: String, value: Any) = mBody.run {
        if (isNotEmpty())
            append("&")
        append("$key=${URLEncoder.encode(value.toString(), "UTF-8")}")
    }

    fun body(params: Array<Pair<String, Any>>) {
        params.map { body(it.first, it.second) }
    }

    fun header(key: String, value: String) = mHeader.put(key, value)

    fun https(ctx: Context, pwdKey: String = "", pemFile: String?, p12File: String) {
        var keyIS: InputStream? = null
        var pemIS: InputStream? = null
        try {
            keyIS = ctx.assets.open(p12File)
            val keyStore = KeyStore.getInstance("PKCS12").apply { load(keyIS, pwdKey.toCharArray()) }
            val kmf = KeyManagerFactory.getInstance("X509").apply { init(keyStore, pwdKey.toCharArray()) }
            if (pemFile.isNullOrBlank()) {
                mSSLContext = SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, null, null) }
                return
            }
            pemIS = ctx.assets.open(pemFile)
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(pemIS) as X509Certificate
            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            val tmf = TrustManagerFactory.getInstance("X509").apply { init(trustStore) }
            trustStore.apply {
                load(null)
                setCertificateEntry(cert.subjectX500Principal.name, cert)
            }
            mSSLContext = SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, tmf.trustManagers, null) }
        } catch (ex: Exception) {
            ex.printStackTrace()
            mSSLContext = null
        } finally {
            try {
                keyIS!!.close()
                pemIS!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun request(isGet: Boolean, ctx: Context, handler: Handler, success: Result?.() -> Unit, failed: String.() -> Unit) {
        if (!NetUtils.hasNet(ctx))
            "无网络连接".failed()
        if (mUrl.isEmpty())
            "空请求".failed()
        ThreadPool.addTask {
            NetCore<Result>().run { if (isGet) doGet(this@Requester, handler, success, failed) else doPost(this@Requester, handler, success, failed) }
        }
    }

    fun get(ctx: Context, handler: Handler, success: Result?.() -> Unit, failed: String.() -> Unit) = request(true, ctx, handler, success, failed)
    fun get(ctx: Context, success: Result?.() -> Unit, failed: String.() -> Unit) = get(ctx, Handler(Looper.getMainLooper()), success, failed)
    fun post(ctx: Context, handler: Handler, success: Result?.() -> Unit, failed: String.() -> Unit) = request(false, ctx, handler, success, failed)
    fun post(ctx: Context, success: Result?.() -> Unit, failed: String.() -> Unit) = post(ctx, Handler(Looper.getMainLooper()), success, failed)
}
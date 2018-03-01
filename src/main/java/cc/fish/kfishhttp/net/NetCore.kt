package cc.fish.kfishhttp.net

import android.accounts.NetworkErrorException
import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.util.Log
import cc.fish.kfishhttp.Requester
import cc.fish.kfishhttp.exception.FishNetException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.CacheRequest
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

/**
 * Created by fish on 18-2-27.
 */
class NetCore<Result> {
    fun doGet(request: Requester<Result>, h: Handler, success: Result?.() -> Unit, failed: String.() -> Unit) {
        getConnetion(request).apply {
            requestMethod = "GET"
            doOutput = false
            connect()
            doNet(request.mType, h, success, failed)
        }
    }

    fun doPost(request: Requester<Result>, h: Handler, success: Result?.() -> Unit, failed: String.() -> Unit) {
        getConnetion(request).apply {
            requestMethod = "POST"
            doOutput = true
            connect()
            outputStream.apply {
                write(request.mBody.toString().toByteArray())
                flush()
                close()
            }
            doNet(request.mType, h, success, failed)
        }
    }

    private fun HttpURLConnection.doNet(type: Class<Result>?, h: Handler, success: Result?.() -> Unit, failed: String.() -> Unit) {
        if (responseCode != 200)
            FishNetException(responseCode).getReasonMessage().failed()
        try {
            val data = IS2Str(inputStream)
            h.post {
                val s = if (type == null) null else Gson().fromJson<Result>(data, type)
                s.success()
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            "I/O错误".failed()
        } catch (ex: TypeCastException) {
            ex.printStackTrace()
            "数据结构错误".failed()
        }
    }

    private fun getConnetion(request: Requester<*>) = URL(request.mUrl.toString()).openConnection().apply {
        if (request.mSSLContext != null && this is HttpsURLConnection)
            sslSocketFactory = request.mSSLContext?.socketFactory
        connectTimeout = 3000
        readTimeout = 100000
        doInput = true
        useCaches = false
        setRequestProperty("Charset", "UTF-8")
        setRequestProperty("Connection", "Keep-Alive")
        setRequestProperty("Content-Type", if (request.mIsJsonBody) "application/json" else "application/x-www-form-urlencoded")
        request.mHeader.map { addRequestProperty(it.key, it.value) }
    } as HttpURLConnection

    private fun IS2Str(inputStream: InputStream): String {
        var reader = InputStreamReader(inputStream, "UTF-8")
        var buf = CharArray(1024)
        var sb = StringBuffer()
        var readCnt: Int
        try {
            do {
                readCnt = reader.read(buf)
                if (readCnt > 0)
                    sb.append(buf, 0, readCnt)
            } while (readCnt >= 0)
            return sb.toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        } finally {
            try {
                reader.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

object NetUtils {
    fun hasNet(ctx: Context) = (ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isAvailable
            ?: false
}
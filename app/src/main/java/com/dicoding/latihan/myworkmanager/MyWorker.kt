package com.dicoding.latihan.myworkmanager

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.text.DecimalFormat

class MyWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {

    companion object {
        private val TAG = MyWorker::class.java.simpleName
        const val API_KEY = "YOUR_KEY_HERE"
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "my weather app channel"
    }

    private var resultStatus: Result? = null

    override fun doWork(): Result {
        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrentDataCity(dataCity)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun showNotification(title: String, description: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun getCurrentDataCity(dataCity: String?): Result {
        Log.d(TAG, "getCurrentDataCity: Multi........")
        Looper.prepare()
        val client = SyncHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$dataCity&appid=${BuildConfig.APP_ID}"
        Log.d(TAG, "getCurrentDataCity: $url")
        client.post(url, object: AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?
            ) {
                val result = responseBody?.let { String(it) }
                Log.d(TAG, "onSuccess: $result")
                try {
                    val responseObject = result?.let { JSONObject(it) }
                    val currentWeather: String =
                        responseObject?.getJSONArray("weather")?.getJSONObject(0)?.getString("main") ?: ""
                    val description: String = responseObject?.getJSONArray("weather")?.getJSONObject(0)?.getString("description")?: ""
                    val tempInKelvin = responseObject?.getJSONObject("main")?.getDouble("temp")
                    val tempInCelsius = tempInKelvin?.minus(273)
                    val temperature: String = DecimalFormat("##.##").format(tempInCelsius)
                    val title = "Current Weather in $dataCity"
                    val message = "$currentWeather, $description with $temperature celsius"
                    showNotification(title, message)
                    Log.d(TAG, "onSuccess: Selesai.....")
                    resultStatus = Result.success()
                }catch (e: Exception) {
                    e.message?.let { showNotification("Get Current Weather Not Success", it) }
                    Log.d(TAG, "onSuccess: Gagal.....")
                    resultStatus = Result.failure()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d(TAG, "onFailure: Gagal.....")
                // ketika proses gagal, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                error?.message?.let { showNotification("Get Current Weather Failed", it) }
                resultStatus = Result.failure()
            }

        })

        return resultStatus as Result
    }
}
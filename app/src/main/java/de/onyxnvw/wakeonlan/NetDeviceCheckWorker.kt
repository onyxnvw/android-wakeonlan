package de.onyxnvw.wakeonlan

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException
import java.net.InetAddress

class NetDeviceCheckWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d(TAG, "NetDeviceCheckWorker doWork entry")
        val attempts = runAttemptCount + 1
        val maxAttempts = inputData.getInt("attempts", 1)
        val host = inputData.getString("host") ?: return Result.failure()
        val timeout = inputData.getInt("timeout", 250)

        return try {
            val inetAddress = InetAddress.getByName(host)
            if (inetAddress.isReachable(timeout)) {
                // host reachable
                Log.d(TAG, "NetDeviceCheckWorker doWork Success")
                Result.success()
            } else {
                // host not reachable
                if (attempts < maxAttempts) {
                    Log.d(TAG, "NetDeviceCheckWorker doWork Retry")
                    Result.retry()
                } else {
                    Log.d(TAG, "NetDeviceCheckWorker doWork Failure")
                    Result.failure()
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, "NetDeviceCheckWorker doWork Failure")
            Result.failure()
        }
    }
}

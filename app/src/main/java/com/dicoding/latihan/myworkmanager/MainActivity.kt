package com.dicoding.latihan.myworkmanager

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dicoding.latihan.myworkmanager.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityMainBinding? = null
    private lateinit var workManager: WorkManager
    private lateinit var periodicWorkRequest: PeriodicWorkRequest

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
        }else {
            Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        workManager = WorkManager.getInstance(this)
        binding?.btnOneTimeTask?.setOnClickListener(this)
        binding?.btnPeriodicTask?.setOnClickListener(this)
        binding?.btnCancelTask?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnOneTimeTask -> startOneTimeTask()
            R.id.btnPeriodicTask -> startPeriodicTask()
            R.id.btnCancelTask -> cancelPeriodicTask()
        }
    }

    private fun cancelPeriodicTask() {
        workManager.cancelWorkById(periodicWorkRequest.id)
    }

    private fun startPeriodicTask() {
        binding?.textStatus?.text = getString(R.string.status)
        val dataBuilder = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding?.editCity?.text.toString())
            .build()
        val constraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class, 15, TimeUnit.MINUTES)
            .setInputData(dataBuilder)
            .setConstraints(constraint)
            .build()

        workManager.enqueue(periodicWorkRequest)
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity) {
                val status = it?.state?.name
                binding?.textStatus?.append("\n$status")
                binding?.btnCancelTask?.isEnabled = false
                if (it?.state == WorkInfo.State.ENQUEUED) {
                    binding?.btnCancelTask?.isEnabled = true
                }
            }
    }

    private fun startOneTimeTask() {
        binding?.textStatus?.text = getString(R.string.status)
        val dataBuilder = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding?.editCity?.text.toString())
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(dataBuilder)
            .setConstraints(constraints)
            .build()
        workManager.enqueue(oneTimeWorkRequest)
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this@MainActivity) {
                val status = it?.state?.name
                binding?.textStatus?.append("\n$status")
            }
    }
}
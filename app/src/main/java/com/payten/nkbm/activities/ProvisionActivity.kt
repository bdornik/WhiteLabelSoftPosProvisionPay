package com.payten.nkbm.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.github.ybq.android.spinkit.SpinKitView
import com.payten.nkbm.R
import com.payten.nkbm.databinding.ActivityPosBinding
import com.payten.nkbm.databinding.ActivityProvisionBinding
import com.payten.nkbm.viewmodel.ProvisionViewModel
import com.sacbpp.remotemanagement.SACBPPNotificationManager
import com.simant.MainApplication
import com.simant.utils.AppEvent
import com.simant.utils.AppEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

import io.reactivex.schedulers.Schedulers

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import javax.inject.Inject


@AndroidEntryPoint
class ProvisionActivity : BaseActivity() {

    private val TAG = "ProvisionActivity"

    private var eventDisposables = CompositeDisposable()

    private lateinit var binding: ActivityProvisionBinding



    private var spinKitView: SpinKitView? = null
    private var cardView : CardView? = null

    private lateinit var provisionTerminalStates: HashMap<Int, String>

    private var provisionStatus  = false;
    private var tickStatus = false;
    private var timeoutSeconds = 15;

    @Inject
    lateinit var sharedPreferences: KsPrefs

    lateinit var model: ProvisionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProvisionBinding.inflate(layoutInflater)

        setContentView(R.layout.activity_provision)
        applyInsetsToRoot(binding.root)

        val model2: ProvisionViewModel by viewModels()
        model = model2

        spinKitView = findViewById(R.id.spinner)
        cardView = findViewById(R.id.pinOverlay)
        spinKitView!!.visibility = View.VISIBLE
        cardView!!.visibility = View.VISIBLE



        startListenBus()
        timeoutProcessHandle()
    }


    override fun onResume() {
        super.onResume()
        model.refreshData()
    }

    private fun startListenBus() {
        Log.i(TAG,"startListenBus()")

        AppEventBus.listen(AppEvent::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                Log.i(TAG, "Event: " + event.toString())
                if (event.process == SACBPPNotificationManager.END_OF_REMOTE_PROCESS) {
                    provisionStatus = true
                    success()
                }
            }
    }




    private fun timeoutProcessHandle() {
        Log.i(TAG,"timeoutProcessHandle()")

        tickStatus = false
        val disposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .takeUntil(object : Predicate<Long> {
                override fun test(t: Long): Boolean {
                    return tickStatus
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer { tick ->
                if (provisionStatus) {
                    tickStatus = true
                    //success()
                } else {
                    tickStatus = tick == timeoutSeconds.toLong()
                    if (tickStatus) {
                        //addState("timeout")
                        Log.i(TAG,"timeOut")
                        failure()
                    }
                }
            }, Consumer {

            })
        eventDisposables.add(disposable)
    }

    private fun failure() {
        Log.i(TAG,"fail()")

        MainApplication.getSACBTPApplication().goOnlineCheckRNS()
        startListenBus()
    }

    private fun success() {
        Log.i(TAG,"success()")
        val intent = Intent(this, LandingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()

    }
}

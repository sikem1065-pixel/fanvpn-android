package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus,
        OpenProxy,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
        RefreshProfile,
        ToggleMode,
        OpenWebsite,
        OpenAnnouncement,
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    suspend fun setProfileName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.profileName = name
        }
    }

    suspend fun setActiveProxy(name: String?) {
        withContext(Dispatchers.Main) {
            binding.activeProxy = name
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running
            setPulsing(running)
        }
    }

    suspend fun setForwarded(value: Long) {
        withContext(Dispatchers.Main) {
            binding.forwarded = value.trafficTotal()
        }
    }

    suspend fun setMode(mode: TunnelState.Mode) {
        withContext(Dispatchers.Main) {
            binding.mode = when (mode) {
                TunnelState.Mode.Global -> "全局模式"
                TunnelState.Mode.Direct -> "直连模式"
                else -> "智能分流"
            }
        }
    }

    suspend fun setHasProviders(has: Boolean) {
        withContext(Dispatchers.Main) {
            binding.hasProviders = has
        }
    }

    suspend fun setRefreshing(refreshing: Boolean) {
        withContext(Dispatchers.Main) {
            binding.isRefreshing = refreshing
        }
    }
    suspend fun setAnnouncement(visible: Boolean, title: String?, body: String?) {
        withContext(Dispatchers.Main) {
            binding.announceTitle = title
            binding.announceBody = body
            binding.announceVisible = visible
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(binding.root)
                .show()
        }
    }

    init {
        binding.self = this
        binding.isRefreshing = false
        binding.mode = "智能分流"
        binding.announceVisible = false

        binding.colorClashStarted = context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)
        binding.colorClashStopped = context.resolveThemedColor(R.attr.colorClashStopped)
    }

    private var pulseSet: android.animation.AnimatorSet? = null

    private fun setPulsing(on: Boolean) {
        pulseSet?.cancel()
        pulseSet = null
        val rings = listOf(binding.ringOuter, binding.ringMid, binding.ringInner)
        if (!on) {
            rings.forEach { it.scaleX = 1f; it.scaleY = 1f; it.alpha = 1f }
            return
        }
        val anims = ArrayList<android.animation.Animator>()
        rings.forEachIndexed { i, v ->
            val delay = (i * 220).toLong()
            anims.add(android.animation.ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.06f).apply {
                duration = 1700; startDelay = delay
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            })
            anims.add(android.animation.ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.06f).apply {
                duration = 1700; startDelay = delay
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            })
            anims.add(android.animation.ObjectAnimator.ofFloat(v, "alpha", 0.9f, 0.5f).apply {
                duration = 1700; startDelay = delay
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            })
        }
        pulseSet = android.animation.AnimatorSet().apply {
            playTogether(anims)
            start()
        }
    }

    fun request(request: Request) {
        requests.trySend(request)
    }
}
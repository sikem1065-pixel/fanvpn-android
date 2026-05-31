package com.github.kr328.clash

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.core.bridge.*
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.TunnelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.R as DesignR

class MainActivity : BaseActivity<MainDesign>() {
    private var announceUrl: String? = null
    override suspend fun main() {

        val design = MainDesign(this)

        setContentDesign(design)

        design.fetch()

        launch {
            ensureDefaultProfile()
            design.fetch()
        }

        launch { fetchAnnouncement(design) }

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop, Event.ClashStart,
                        Event.ProfileLoaded, Event.ProfileChanged -> design.fetch()
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        MainDesign.Request.ToggleStatus -> {
                            if (clashRunning)
                                stopClashService()
                            else
                                design.startClash()
                        }
                        MainDesign.Request.OpenProxy -> {
                            if (clashRunning) {
                                startActivity(ProxyActivity::class.intent)
                            } else {
                                design.showToast(DesignR.string.fanvpn_connect_first, ToastDuration.Long)
                            }
                        }
                        MainDesign.Request.OpenProfiles ->
                            startActivity(ProfilesActivity::class.intent)
                        MainDesign.Request.OpenProviders ->
                            startActivity(ProvidersActivity::class.intent)
                        MainDesign.Request.OpenLogs -> {
                            if (LogcatService.running) {
                                startActivity(LogcatActivity::class.intent)
                            } else {
                                startActivity(LogsActivity::class.intent)
                            }
                        }
                        MainDesign.Request.OpenSettings ->
                            startActivity(SettingsActivity::class.intent)
                        MainDesign.Request.OpenHelp ->
                            startActivity(HelpActivity::class.intent)
                        MainDesign.Request.ToggleMode -> {
                            val current = withClash { queryTunnelState().mode }
                            val next = if (current == TunnelState.Mode.Global)
                                TunnelState.Mode.Rule else TunnelState.Mode.Global
                            withClash {
                                val o = queryOverride(Clash.OverrideSlot.Session)
                                o.mode = next
                                patchOverride(Clash.OverrideSlot.Session, o)
                            }
                            design.fetch()
                        }
                        MainDesign.Request.OpenWebsite -> {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://fanvpn.net")))
                            } catch (e: Exception) {
                            }
                        }
                        MainDesign.Request.OpenAnnouncement -> {
                            announceUrl?.let {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it)))
                                } catch (e: Exception) {
                                }
                            }
                        }
                        MainDesign.Request.OpenAbout ->
                            design.showAbout(queryAppVersionName())
                        MainDesign.Request.RefreshProfile -> {
                            val target = withProfile { queryActive() ?: queryAll().firstOrNull() }
                            if (target != null) {
                                design.setRefreshing(true)
                                try {
                                    withProfile { update(target.uuid); setActive(target) }
                                    design.showToast(DesignR.string.fanvpn_refresh_done, ToastDuration.Short)
                                } catch (e: Exception) {
                                    android.util.Log.e("FanVPN", "refresh FAILED: ${e.message}", e)
                                    design.showToast(DesignR.string.fanvpn_refresh_failed, ToastDuration.Long)
                                } finally {
                                    design.setRefreshing(false)
                                    design.fetch()
                                }
                            }
                        }
                    }
                }
                if (clashRunning) {
                    ticker.onReceive {
                        design.fetchTraffic()
                    }
                }
            }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(clashRunning)

        val state = withClash {
            queryTunnelState()
        }
        val providers = withClash {
            queryProviders()
        }

        setMode(state.mode)
        setHasProviders(providers.isNotEmpty())

        withProfile {
            setProfileName(queryActive()?.name)
        }

        val active = if (clashRunning) queryActiveProxyName() else null
        if (active != null) uiStore.lastProxyName = active
        setActiveProxy(active ?: uiStore.lastProxyName.ifEmpty { null })
    }

    private suspend fun MainDesign.fetchTraffic() {
        withClash {
            setForwarded(queryTrafficTotal())
        }
        queryActiveProxyName()?.let {
            uiStore.lastProxyName = it
            setActiveProxy(it)
        }
    }

    private suspend fun queryActiveProxyName(): String? {
        return withClash {
            val names = queryProxyGroupNames(false)
            if (names.isEmpty()) return@withClash null

            var groupName = if (names.contains("PROXY")) "PROXY" else names.first()
            var now = queryProxyGroup(groupName, uiStore.proxySort).now

            var guard = 0
            while (now.isNotEmpty() && names.contains(now) && now != groupName && guard < 5) {
                groupName = now
                now = queryProxyGroup(groupName, uiStore.proxySort).now
                guard++
            }

            now.ifEmpty { null }
        }
    }

    private suspend fun MainDesign.startClash() {
        val active = withProfile { queryActive() }

        if (active == null || !active.imported) {
            showToast(DesignR.string.no_profile_selected, ToastDuration.Long) {
                setAction(DesignR.string.profiles) {
                    startActivity(ProfilesActivity::class.intent)
                }
            }

            return
        }

        val vpnRequest = startClashService()

        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    vpnRequest
                )

                if (result.resultCode == RESULT_OK)
                    startClashService()
            }
        } catch (e: Exception) {
            design?.showToast(DesignR.string.unable_to_start_vpn, ToastDuration.Long)
        }
    }

    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName + "\n" + Bridge.nativeCoreVersion().replace("_", "-")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupShortcuts()
    }

    private suspend fun fetchAnnouncement(design: MainDesign) {
        val urls = listOf(
            BuildConfig.CFG_BASE_1,
            BuildConfig.CFG_BASE_2,
            BuildConfig.CFG_BASE_3
        ).filter { it.isNotEmpty() }.map { "$it/announce.json" }
        val text = withContext(Dispatchers.IO) {
            var result: String? = null
            for (u in urls) {
                try {
                    val conn = java.net.URL(u).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    result = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    break
                } catch (e: Exception) {
                    android.util.Log.e("FanVPN", "announce fetch fail: ${e.message}")
                }
            }
            result
        }
        if (text == null) return
        try {
            val o = org.json.JSONObject(text)
            if (!o.optBoolean("show", true)) {
                design.setAnnouncement(false, null, null)
                return
            }
            announceUrl = o.optString("url", "").ifEmpty { null }
            design.setAnnouncement(true, o.optString("title", "公告"), o.optString("body", ""))
        } catch (e: Exception) {
            android.util.Log.e("FanVPN", "announce parse fail: ${e.message}")
        }
    }

    private suspend fun ensureDefaultProfile() {
        withProfile {
            if (queryAll().isNotEmpty()) return@withProfile
            kotlinx.coroutines.delay(2500)

            val urls = listOf(
                BuildConfig.CFG_BASE_1,
                BuildConfig.CFG_BASE_2,
                BuildConfig.CFG_BASE_3
            ).filter { it.isNotEmpty() }.map { "$it/android.yaml" }

            val uuid = create(
                type = Profile.Type.Url,
                name = "FanVPN",
                source = urls[0]
            )

            var success = false
            repeat(2) {
                if (!success) {
                    for (url in urls) {
                        try {
                            patch(uuid, "FanVPN", url, java.util.concurrent.TimeUnit.MINUTES.toMillis(1440))
                            commit(uuid)
                            success = true
                            break
                        } catch (e: Exception) {
                            android.util.Log.e("FanVPN", "update FAILED: ${e.message}", e)
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                    if (!success) kotlinx.coroutines.delay(2000)
                }
            }
            if (success) {
                queryByUUID(uuid)?.let { setActive(it) }
            }
        }
    }

    private fun setupShortcuts() {
        // Skip dynamic shortcut setup when the app icon is hidden.
        if (uiStore.hideAppIcon) return

        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
            Intent.FLAG_ACTIVITY_NO_ANIMATION

        val toggle = ShortcutInfoCompat.Builder(this, "toggle_clash")
            .setShortLabel(getString(DesignR.string.shortcut_toggle_short))
            .setLongLabel(getString(DesignR.string.shortcut_toggle_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_all))
            .setIntent(
                Intent(Intents.ACTION_TOGGLE_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(0)
            .build()

        val start = ShortcutInfoCompat.Builder(this, "start_clash")
            .setShortLabel(getString(DesignR.string.shortcut_start_short))
            .setLongLabel(getString(DesignR.string.shortcut_start_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_on))
            .setIntent(
                Intent(Intents.ACTION_START_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(1)
            .build()

        val stop = ShortcutInfoCompat.Builder(this, "stop_clash")
            .setShortLabel(getString(DesignR.string.shortcut_stop_short))
            .setLongLabel(getString(DesignR.string.shortcut_stop_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_off))
            .setIntent(
                Intent(Intents.ACTION_STOP_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(2)
            .build()

        ShortcutManagerCompat.setDynamicShortcuts(this, listOf(toggle, start, stop))
    }
}

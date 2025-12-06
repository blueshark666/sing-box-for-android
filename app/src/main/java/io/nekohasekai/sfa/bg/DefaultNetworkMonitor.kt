package io.nekohasekai.sfa.bg

import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.constant.Bugs
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.database.ProfileManager
import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.NetworkInterface

object DefaultNetworkMonitor {

    var defaultNetwork: Network? = null
    private var listener: InterfaceUpdateListener? = null
    private var currentNetworkType: NetworkType = NetworkType.UNKNOWN
    private const val SWITCH_DELAY = 3000L // 切换延迟3秒，用于防抖确认
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingSwitchRunnable: Runnable? = null
    
    // 网络类型到配置文件ID的映射缓存，避免每次网络变化都查找配置文件
    private val networkProfileMap = mutableMapOf<NetworkType, Long>()
    
    // 配置文件更新监听器，当配置文件发生变化时更新映射缓存
    private val profileUpdateCallback: () -> Unit = { 
        GlobalScope.launch(Dispatchers.IO) {
            updateNetworkProfileMap()
        }
    }

    enum class NetworkType {
        WIFI,
        MOBILE,
        OTHER,
        UNKNOWN
    }

    suspend fun start() {
        // 初始化网络类型到配置文件的映射缓存
        updateNetworkProfileMap()
        
        // 注册配置文件更新监听器
        ProfileManager.registerCallback(profileUpdateCallback)
        
        DefaultNetworkListener.start(this) {
            defaultNetwork = it
            val newNetworkType = getNetworkType(it)

            if (newNetworkType != currentNetworkType) {
                onNetworkTypeChanged(newNetworkType)
            }

            checkDefaultInterfaceUpdate(it)
        }

        defaultNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Application.connectivity.activeNetwork
        } else {
            DefaultNetworkListener.get()
        }
        currentNetworkType = getNetworkType(defaultNetwork)
        // 服务启动时立即根据当前网络类型切换配置
        onNetworkTypeChanged(currentNetworkType)
    }

    suspend fun stop() {
        DefaultNetworkListener.stop(this)
        // 移除配置文件更新监听器
        ProfileManager.unregisterCallback(profileUpdateCallback)
    }

    suspend fun require(): Network {
        val network = defaultNetwork
        if (network != null) {
            return network
        }
        return DefaultNetworkListener.get()
    }

    fun setListener(listener: InterfaceUpdateListener?) {
        this.listener = listener
        checkDefaultInterfaceUpdate(defaultNetwork)
    }
    
    // 更新网络类型到配置文件的映射缓存
    private suspend fun updateNetworkProfileMap() {
        val allProfiles = ProfileManager.list()
        
        // 查找WiFi对应的配置文件
        networkProfileMap[NetworkType.WIFI] = allProfiles.find { it.name.equals("wifi", ignoreCase = true) }?.id ?: 
                                         allProfiles.find { it.name.contains("wifi", ignoreCase = true) }?.id ?: -1L
        
        // 查找移动网络对应的配置文件
        networkProfileMap[NetworkType.MOBILE] = allProfiles.find { it.name.equals("cellular", ignoreCase = true) }?.id ?: 
                                           allProfiles.find { it.name.contains("cellular", ignoreCase = true) }?.id ?: -1L
        
        android.util.Log.d("DefaultNetworkMonitor", "网络配置映射已更新: WiFi=${networkProfileMap[NetworkType.WIFI]}, MOBILE=${networkProfileMap[NetworkType.MOBILE]}")
    }

    private fun checkDefaultInterfaceUpdate(
        newNetwork: Network?
    ) {
        val listener = listener ?: return
        if (newNetwork != null) {
            val interfaceName =
                (Application.connectivity.getLinkProperties(newNetwork) ?: return).interfaceName
            for (times in 0 until 10) {
                var interfaceIndex: Int
                try {
                    interfaceIndex = NetworkInterface.getByName(interfaceName).index
                } catch (e: Exception) {
                    Thread.sleep(100)
                    continue
                }
                if (Bugs.fixAndroidStack) {
                    GlobalScope.launch(Dispatchers.IO) {
                        listener.updateDefaultInterface(interfaceName, interfaceIndex, false, false)
                    }
                } else {
                    listener.updateDefaultInterface(interfaceName, interfaceIndex, false, false)
                }
            }
        } else {
            if (Bugs.fixAndroidStack) {
                GlobalScope.launch(Dispatchers.IO) {
                    listener.updateDefaultInterface("", -1, false, false)
                }
            } else {
                listener.updateDefaultInterface("", -1, false, false)
            }
        }
    }

    private fun getNetworkType(network: Network?): NetworkType {
        if (network == null) return NetworkType.UNKNOWN
        
        val networkCapabilities = Application.connectivity.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN
        
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkType.WIFI
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkType.MOBILE
        }
        
        return NetworkType.OTHER
    }

    private fun onNetworkTypeChanged(newNetworkType: NetworkType) {
        // 全部切到主线程，保证 handler 和 runnable 操作在同一线程
        GlobalScope.launch(Dispatchers.Main) {

            // 检查网络类型
            if (newNetworkType == NetworkType.OTHER || newNetworkType == NetworkType.UNKNOWN) {
                android.util.Log.d("DefaultNetworkMonitor", "网络类型无效($newNetworkType)，取消切换")
                return@launch
            }

            // 查找预缓存的配置文件
            val targetProfileId = networkProfileMap[newNetworkType]
            if (targetProfileId == null || targetProfileId == -1L) {
                android.util.Log.d("DefaultNetworkMonitor", "未找到 $newNetworkType 对应配置文件")
                return@launch
            }

            // 已经是当前 profile，无需切换
            if (targetProfileId == Settings.selectedProfile) {
                android.util.Log.d("DefaultNetworkMonitor", "当前已是 $newNetworkType 对应配置文件，无需切换")
                return@launch
            }

            // 取消之前的防抖任务（必须在主线程才能生效）
            pendingSwitchRunnable?.let { r ->
                handler.removeCallbacks(r)
                android.util.Log.d("DefaultNetworkMonitor", "取消之前的切换任务")
            }

            // 创建新的切换任务（在主线程创建 & 运行 handler）
            val switchRunnable = Runnable {
                GlobalScope.launch(Dispatchers.IO) {

                    val latestType = getNetworkType(defaultNetwork)
                    android.util.Log.d("DefaultNetworkMonitor", "3秒后最新网络类型 = $latestType")

                    if (latestType == NetworkType.OTHER || latestType == NetworkType.UNKNOWN) {
                        pendingSwitchRunnable = null
                        return@launch
                    }

                    val latestProfileId = networkProfileMap[latestType] ?: -1L
                    if (latestProfileId == -1L || latestProfileId == Settings.selectedProfile) {
                        pendingSwitchRunnable = null
                        return@launch
                    }

                    // 切换
                    Settings.selectedProfile = latestProfileId
                    currentNetworkType = latestType

                    android.util.Log.d(
                        "DefaultNetworkMonitor",
                        "执行切换 → Profile $latestProfileId ($latestType)"
                    )

                    // 通知 UI
                    GlobalScope.launch(Dispatchers.Main) {
                        ProfileManager.triggerCallbacks()
                    }

                    // reload
                    Libbox.newStandaloneCommandClient().serviceReload()

                    pendingSwitchRunnable = null
                }
            }

            // 保存 runnable（必须在主线程）
            pendingSwitchRunnable = switchRunnable

            // 启动 3 秒防抖
            handler.postDelayed(switchRunnable, SWITCH_DELAY)
            android.util.Log.d("DefaultNetworkMonitor", "网络变为 $newNetworkType，等待3秒防抖…")
        }
    }
}
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

    enum class NetworkType {
        WIFI,
        MOBILE,
        OTHER,
        UNKNOWN
    }

    suspend fun start() {
        DefaultNetworkListener.start(this) {
            defaultNetwork = it
            val newNetworkType = getNetworkType(it)
            if (newNetworkType != currentNetworkType) {
                currentNetworkType = newNetworkType
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
    }

    suspend fun stop() {
        DefaultNetworkListener.stop(this)
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
        GlobalScope.launch(Dispatchers.IO) {
            // 获取所有配置文件
            val allProfiles = ProfileManager.list()
            
            // 根据网络类型选择对应的配置文件
            val targetProfile = when (newNetworkType) {
                NetworkType.WIFI -> {
                    // 优先查找名称完全匹配"wifi"的配置文件（不区分大小写）
                    allProfiles.find { profile ->
                        profile.name.equals("wifi", ignoreCase = true)
                    } ?: // 如果没有完全匹配的，查找名称包含"wifi"的配置文件（不区分大小写）
                    allProfiles.find { profile ->
                        profile.name.contains("wifi", ignoreCase = true)
                    }
                }
                NetworkType.MOBILE -> {
                    // 优先查找名称完全匹配"cellular"的配置文件（不区分大小写）
                    allProfiles.find { profile ->
                        profile.name.equals("cellular", ignoreCase = true)
                    } ?: // 如果没有完全匹配的，查找名称包含"cellular"的配置文件（不区分大小写）
                    allProfiles.find { profile ->
                        profile.name.contains("cellular", ignoreCase = true)
                    }
                }
                else -> return@launch // 不处理其他网络类型
            }

            // 检查是否找到匹配的配置文件
            if (targetProfile == null) return@launch

            // 检查是否需要切换
            if (targetProfile.id == Settings.selectedProfile) return@launch

            // 更新当前选中的配置文件
            Settings.selectedProfile = targetProfile.id

            Libbox.newStandaloneCommandClient().serviceReload()
        }
    }

}
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
import kotlinx.coroutines.delay


object DefaultNetworkMonitor {

    var defaultNetwork: Network? = null
    private var listener: InterfaceUpdateListener? = null
    private var currentNetworkType: NetworkType = NetworkType.UNKNOWN
    
    // 存储网络类型对应的配置文件ID，只在启动时查找一次
    private var wifiProfileId: Long? = null
    private var mobileProfileId: Long? = null

    enum class NetworkType {
        WIFI,
        MOBILE,
        OTHER,
        UNKNOWN
    }

    suspend fun start() {
        // 启动时查找网络类型对应的配置文件并保存
        findNetworkProfiles()
        
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
        // 服务启动时立即根据当前网络类型切换配置
        onNetworkTypeChanged(currentNetworkType)
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

    private fun checkDefaultInterfaceUpdate(newNetwork: Network?) {
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
                listener.updateDefaultInterface(interfaceName, interfaceIndex, false, false)
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

    // 在启动时查找并保存网络类型对应的配置文件
    private suspend fun findNetworkProfiles() {
        // 获取所有配置文件
        val allProfiles = ProfileManager.list()
        
        // 查找WiFi配置文件
        wifiProfileId = allProfiles.find { profile ->
            profile.name.equals("wifi", ignoreCase = true)
        }?.id ?: allProfiles.find { profile ->
            profile.name.contains("wifi", ignoreCase = true)
        }?.id
        
        // 查找移动网络配置文件
        mobileProfileId = allProfiles.find { profile ->
            profile.name.equals("cellular", ignoreCase = true)
        }?.id ?: allProfiles.find { profile ->
            profile.name.contains("cellular", ignoreCase = true)
        }?.id
    }

    private fun onNetworkTypeChanged(newNetworkType: NetworkType) {
        GlobalScope.launch(Dispatchers.IO) {
            // 根据网络类型获取对应的配置文件ID
            val targetProfileId = when (newNetworkType) {
                NetworkType.WIFI -> wifiProfileId
                NetworkType.MOBILE -> mobileProfileId
                else -> return@launch // 不处理其他网络类型
            }

            // 检查是否找到匹配的配置文件
            if (targetProfileId == null) return@launch

            // 检查是否需要切换
            if (targetProfileId == Settings.selectedProfile) return@launch

            // 更新当前选中的配置文件
            Settings.selectedProfile = targetProfileId

            delay(200)

            // 通知UI配置文件变化
            GlobalScope.launch(Dispatchers.Main) {
                ProfileManager.triggerCallbacks()
            }

            Libbox.newStandaloneCommandClient().serviceReload()
        }
    }

}

package net.corekit.monetize.ads.report

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.ext.DataStoreStringDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.log.AdLogger
import java.io.IOException

/**
 * FPU上报控制器
 * 负责处理广告填充次数上报逻辑
 */
@SuppressLint("StaticFieldLeak")
object FpuController {
    private const val TAG = "FPU配置"
    private const val CONFIG_FILE_NAME = "fpu_config.json"
    private const val REMOTE_CONFIG_KEY = "adfill_target_fpu"
    
    // 累积填充计数（持久化）
    private var totalFillCount by DataStoreIntDelegate("fpu_total_fills", 0)
    
    // 远程配置JSON（持久化）
    private var remoteConfigJson by DataStoreStringDelegate("fpu_remote_config_json", "")
    
    // 配置数据
    private var configs: List<FpuReportConfig> = emptyList()
    private var context: Context? = null
    
    /**
     * 初始化FPU上报控制器
     * @param context 应用上下文
     */
    fun initialize(context: Context) {
        this.context = context
        loadConfig()
    }
    
    /**
     * 加载配置
     */
    private fun loadConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 尝试获取远程配置
                val newRemoteConfigJson = ConfigRemoteManager.getString(REMOTE_CONFIG_KEY)
                
                if (!newRemoteConfigJson.isNullOrEmpty()) {
                    AdLogger.d("$TAG: 获取到新的远程配置")
                    // 保存远程配置到本地
                    remoteConfigJson = newRemoteConfigJson
                    configs = parseConfigs(newRemoteConfigJson)
                } else {
                    // 2. 尝试使用持久化的远程配置
                    if (remoteConfigJson.orEmpty().isNotEmpty()) {
                        AdLogger.d("$TAG: 使用持久化的远程配置")
                        configs = parseConfigs(remoteConfigJson.orEmpty())
                    } else {
                        // 3. 使用本地默认配置
                        AdLogger.d("$TAG: 使用本地默认配置")
                        configs = loadLocalConfigs()
                    }
                }
                
                AdLogger.d("$TAG: 配置加载成功，共 ${configs.size} 个配置")
                configs.forEach { config ->
                    AdLogger.d("$TAG: 配置 - name: ${config.name}, enabled: ${config.enabled}, fpu: ${config.fpu}")
                }
                
            } catch (e: Exception) {
                AdLogger.e("$TAG: 配置加载失败", e)
                // 降级到本地配置
                configs = loadLocalConfigs()
            }
        }
    }
    
    /**
     * 加载本地配置
     */
    private fun loadLocalConfigs(): List<FpuReportConfig> {
        return try {
            context?.assets?.open(CONFIG_FILE_NAME)?.use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                parseConfigs(json)
            } ?: emptyList()
        } catch (e: IOException) {
            AdLogger.e("$TAG: 读取本地配置文件失败", e)
            emptyList()
        }
    }
    
    /**
     * 解析配置JSON数组
     */
    private fun parseConfigs(json: String): List<FpuReportConfig> {
        return try {
            val configList = Gson().fromJson(json, Array<FpuReportConfig>::class.java)
            configList.filter { config ->
                if (validateConfig(config)) {
                    true
                } else {
                    AdLogger.e("$TAG: 配置参数校验失败 - name: ${config.name}")
                    false
                }
            }
        } catch (e: JsonSyntaxException) {
            AdLogger.e("$TAG: 配置JSON解析失败", e)
            emptyList()
        }
    }
    
    /**
     * 校验配置参数
     */
    private fun validateConfig(config: FpuReportConfig): Boolean {
        return try {
            // 校验必填字段
            if (config.name.isNullOrBlank()) {
                AdLogger.e("$TAG: 配置name字段不能为空")
                return false
            }
            
            // 校验数值字段
            if (config.fpu != null && config.fpu < 0) {
                AdLogger.e("$TAG: 配置fpu字段不能为负数")
                return false
            }
            
            // 校验广告类型
            if (!config.ad_types.isNullOrEmpty()) {
                val validAdTypes = setOf("SP", "IV", "NA", "RV", "BA")
                for (adType in config.ad_types) {
                    if (!validAdTypes.contains(adType)) {
                        AdLogger.e("$TAG: 无效的广告类型: $adType")
                        return false
                    }
                }
            }
            
            AdLogger.d("$TAG: 配置参数校验通过")
            true
        } catch (e: Exception) {
            AdLogger.e("$TAG: 配置校验异常", e)
            false
        }
    }
    
    /**
     * 处理广告填充事件
     * @param adType 广告类型 (SP: 开屏, IV: 插屏, NA: Native, RV: 激励视频, BA: Banner)
     */
    fun onAdFill(adType: String) {
        if (configs.isEmpty()) {
            AdLogger.w("$TAG: 配置未加载，跳过上报")
            return
        }
        
        // 累积填充次数
        totalFillCount++
        
        AdLogger.d("$TAG: 累积填充次数: $totalFillCount")
        
        // 遍历所有配置，检查是否有匹配的配置需要上报
        configs.forEach { config ->
            // 检查配置是否启用
            if (config.enabled == false) {
                AdLogger.d("$TAG: 配置 ${config.name} 已禁用，跳过")
                return@forEach
            }
            
            // 检查广告类型是否在配置的列表中
            if (!config.ad_types.isNullOrEmpty() && !config.ad_types.contains(adType)) {
                AdLogger.d("$TAG: 配置 ${config.name} 不包含广告类型 $adType，跳过")
                return@forEach
            }
            
            // 检查是否达到FPU阈值
            val fpuThreshold = config.fpu ?: 0
            if (fpuThreshold > 0 && totalFillCount >= fpuThreshold) {
                // 触发上报
                reportEvent(config)
                
                // 根据配置决定是否重置计数
                if (config.reset_after_trigger == true) {
                    totalFillCount = 0
                    AdLogger.d("$TAG: 配置 ${config.name} 触发重置累积填充计数")
                }
            } else if (fpuThreshold == 0) {
                // FPU为0表示不限制，直接上报
                reportEvent(config)
            }
        }
    }
    
    /**
     * 上报事件
     */
    private fun reportEvent(config: FpuReportConfig) {
        try {
            val eventData = mapOf(
                "name" to (config.name ?: ""),
                "enabled" to (config.enabled ?: false),
                "fpu" to (config.fpu ?: 0),
                "reset_after_trigger" to (config.reset_after_trigger ?: false),
                "ad_types" to (config.ad_types ?: emptyList()),
                "fill_count" to totalFillCount
            )
            
            val eventName = config.name.orEmpty()
            ReportDataManager.reportData(eventName, eventData)
            AdLogger.d("$TAG: 上报事件成功 - $eventName, 数据: $eventData")
            
        } catch (e: Exception) {
            AdLogger.e("$TAG: 上报事件失败", e)
        }
    }
}

/**
 * FPU上报配置数据类
 */
data class FpuReportConfig(
    @SerializedName("name")
    val name: String?,                   // 上报事件名
    
    @SerializedName("enabled")
    val enabled: Boolean?,               // 目标事件触发功能开关
    
    @SerializedName("fpu")
    val fpu: Int?,                       // 每用户填充次数阈值
    
    @SerializedName("ad_types")
    val ad_types: List<String>?,         // 适用的广告类型列表
    
    @SerializedName("reset_after_trigger")
    val reset_after_trigger: Boolean?    // 是否在触发后重置计数
)

/*
 * Copyright 2023 Google LLC
 *
 * 根据 Apache License 2.0 版本授权使用本文件。
 * 除非遵守许可证，否则不得使用本文件。
 * 许可证副本可在以下地址获取：
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则根据许可证分发的软件
 * 按"原样"提供，不附带任何明示或暗示的保证或条件。
 * 请参阅许可证了解具体的权限和限制。
 */

package net.corekit.monetize.ump

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.also
import kotlin.coroutines.resume
import kotlin.getOrDefault
import kotlin.runCatching

/**
 * Google Mobile Ads SDK 同意管理器
 *
 * Google Mobile Ads SDK 提供用户消息平台（UMP，Google 的 IAB 认证同意管理平台）
 * 作为在 GDPR 影响国家/地区获取用户同意的解决方案之一。
 * 这是一个示例实现，您也可以选择其他同意管理平台来获取用户同意。
 *
 * 主要功能：
 * 1. 请求并更新用户同意信息
 * 2. 在需要时加载并显示同意表单
 * 3. 提供隐私选项表单入口
 * 4. 判断是否可以请求广告
 *
 * 使用说明：
 * - 每次应用启动时都应调用 gatherConsent() 请求更新同意信息
 * - UMP SDK 会自动检测用户地理位置并决定是否需要显示同意弹窗
 * - 在非 GDPR 地区，弹窗不会显示，但仍会更新同意状态
 *
 * 调试说明：
 * - 在非 EEA 地区测试时，需要启用调试模式才能看到弹窗
 * - 使用 setDebugGeography(DEBUG_GEOGRAPHY_EEA) 强制模拟 EEA 地区
 * - 需要添加测试设备的哈希 ID（从 Logcat 获取）
 */
class GoogleMobileAdsConsentManager private constructor(context: Context) {
  private val consentInformation: ConsentInformation =
    UserMessagingPlatform.getConsentInformation(context)

  /** 
   * 同意收集完成回调接口
   * 当同意信息收集流程完成时调用（无论成功或失败）
   */
  fun interface OnConsentGatheringCompleteListener {
    /**
     * 同意收集完成回调
     * @param error 如果发生错误则为错误信息，成功时为 null
     */
    fun consentGatheringComplete(error: FormError?)
  }

  /** 
   * 判断应用是否可以请求广告
   * 在用户同意或不需要同意的情况下返回 true
   */
  val canRequestAds: Boolean
    get() = consentInformation.canRequestAds()

  // [START is_privacy_options_required]
  /** 
   * 判断是否需要显示隐私选项入口
   * 当用户在 GDPR 地区且需要提供修改隐私设置的入口时返回 true
   * 可用于在设置页面显示"隐私设置"按钮
   */
  val isPrivacyOptionsRequired: Boolean
    get() =
      consentInformation.privacyOptionsRequirementStatus ==
        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

  // [END is_privacy_options_required]

  /**
   * 挂起函数版本的同意收集
   *
   * 此方法会：
   * 1. 请求更新同意信息（联网查询用户地理位置和同意状态）
   * 2. 如果需要，加载并显示同意表单
   * 3. 等待用户操作完成
   *
   * @param activity 用于显示同意表单的 Activity
   * @return true 表示可以请求广告，false 表示不可以或发生错误
   *
   * 注意：
   * - 此方法应在每次应用启动时调用
   * - 在非 GDPR 地区不会显示弹窗，但仍会更新状态
   * - 如果用户之前已同意，也不会重复显示弹窗
   */
  suspend fun gatherConsent(
    activity: Activity,
  ): Boolean = runCatching {
      suspendCancellableCoroutine { continuation ->
          gatherConsent(activity) { error ->
              if (error != null) {
                  // 有错误时返回false
                  continuation.resume(false)
              } else {
                  // 成功时返回canRequestAds状态
                  continuation.resume(canRequestAds)
              }
          }
      }
  }.getOrDefault(true)

  /**
   * 回调版本的同意收集方法
   *
   * 调用 UMP SDK 请求同意信息，并在需要时加载/显示同意表单。
   *
   * @param activity 用于显示同意表单的 Activity
   * @param onConsentGatheringCompleteListener 同意收集完成回调
   *
   * 调试模式说明：
   * - 取消注释下方的 debugSettings 代码块可启用调试模式
   * - DEBUG_GEOGRAPHY_EEA: 强制模拟 EEA 地区（会显示 GDPR 弹窗）
   * - DEBUG_GEOGRAPHY_NOT_EEA: 强制模拟非 EEA 地区（不显示弹窗）
   * - 测试设备哈希 ID 可从 Logcat 中搜索 "Use new ConsentDebugSettings" 获取
   */
  fun gatherConsent(
    activity: Activity,
    onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener,
  ) {
    // ========== 调试设置（正式发布时应注释掉）==========
    // 用于测试目的，可以强制设置地理位置为 EEA 或非 EEA
    // 步骤：
    // 1. 运行应用，在 Logcat 中搜索 "Use new ConsentDebugSettings" 获取设备哈希 ID
    // 2. 将哈希 ID 填入 addTestDeviceHashedId()
    // 3. 取消注释以下代码块
    //
//     val debugSettings = ConsentDebugSettings.Builder(activity)
//         .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//         .addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID") // 替换为你的设备哈希ID
//         .build()
    // ========== 调试设置结束 ==========

    val params = ConsentRequestParameters.Builder()
//         .setConsentDebugSettings(debugSettings) // 调试时取消注释
        .build()

    // [START request_consent_info_update]
    // 每次应用启动时都应请求更新同意信息
    // UMP SDK 会自动检测用户地理位置并决定是否需要同意
    consentInformation.requestConsentInfoUpdate(
      activity,
      params,
      {
        // 同意信息更新成功回调
        // 继续加载并显示同意表单（如果需要）
        loadAndShowConsentFormIfRequired(activity, onConsentGatheringCompleteListener)
      },
      { requestConsentError ->
        // 更新同意信息时发生错误
        // 可能原因：网络问题、SDK 配置错误等
        onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
      },
    )
    // [END request_consent_info_update]
  }

  /**
   * 加载并显示同意表单（如果需要）
   *
   * UMP SDK 会根据以下条件决定是否显示表单：
   * - 用户是否在 GDPR 影响地区
   * - 用户之前是否已经做出同意选择
   * - 同意状态是否需要更新
   *
   * 如果不需要显示表单，此方法会立即返回成功
   */
  private fun loadAndShowConsentFormIfRequired(
    activity: Activity,
    onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener,
  ) {
    // [START load_and_show_consent_form]
    // 加载并显示同意表单（如果需要）
    // 如果用户不在 GDPR 地区或已经同意，不会显示任何弹窗
    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
      // 同意收集流程完成
      // formError 为 null 表示成功，否则表示发生错误
      onConsentGatheringCompleteListener.consentGatheringComplete(formError)
    }
    // [END load_and_show_consent_form]
  }

  /**
   * 显示隐私选项表单
   *
   * 用于在设置页面让用户修改隐私偏好设置。
   * 只有在 isPrivacyOptionsRequired 为 true 时才应显示此选项。
   *
   * @param activity FragmentActivity 实例
   * @param onConsentFormDismissedListener 表单关闭回调
   */
  fun showPrivacyOptionsForm(
    activity: FragmentActivity,
    onConsentFormDismissedListener: OnConsentFormDismissedListener,
  ) {
    // [START present_privacy_options_form]
    // 显示隐私选项表单，允许用户修改同意设置
    UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    // [END present_privacy_options_form]
  }

  companion object {
    @Volatile
    private var instance: GoogleMobileAdsConsentManager? = null

    fun getInstance(context: Context) =
      instance
        ?: synchronized(this) {
            instance ?: GoogleMobileAdsConsentManager(context).also { instance = it }
        }
  }
}

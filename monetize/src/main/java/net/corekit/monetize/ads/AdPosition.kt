package net.corekit.monetize.ads

/**
 * 广告位Position常量定义
 * 用于广告展示时的埋点上报
 * 
 * 命名规则:
 * - SP_ : 开屏广告 (Splash)
 * - IV_ : 插屏广告 (Interstitial Video)
 * - RV_ : 激励广告 (Rewarded Video)
 * - NA_ : 原生广告 (Native Ad)
 * - BA_ : 横幅广告 (Banner Ad)
 */
object AdPosition {
    // ==================== 开屏广告 ====================
    /** APP冷热启动时 */
    val SP_APP_START = "SP_AppStart"
    
    // ==================== 插屏广告 - 录入页面返回 ====================
    /** 从血糖录入新数据界面返回时 */
    val IV_BLOOD_SUGAR_BACK = "IV_BloodSugar_back"
    /** 从血压录入新数据界面返回时 */
    val IV_BLOOD_PRESSURE_BACK = "IV_BloodPressure_back"
    /** 从胆固醇录入新数据界面返回时 */
    val IV_CHOLESTEROL_BACK = "IV_Cholesterol_back"
    /** 从心率录入新数据界面返回时 */
    val IV_HEART_RATE_BACK = "IV_HeartRate_back"
    /** 从体重BMI录入新数据界面返回时 */
    val IV_BMI_BACK = "IV_BMI_back"
    /** 从喝水录入新数据界面返回时 */
    val IV_WATER_BACK = "IV_Water_back"
    /** 从走路录入新数据界面返回时 */
    val IV_WALK_BACK = "IV_Walk_back"
    /** 从计步设置界面返回时 */
    val IV_STEP_SETTING_BACK = "IV_Step_Setting_back"

    // ==================== 插屏广告 - 录入页面保存 ====================
    /** 从血糖录入新数据界面点击保存数据时 */
    val IV_BLOOD_SUGAR_SAVE = "IV_BloodSugar_Save"
    /** 从血压录入新数据界面点击保存数据时 */
    val IV_BLOOD_PRESSURE_SAVE = "IV_BloodPressure_Save"
    /** 从胆固醇录入新数据界面点击保存数据时 */
    val IV_CHOLESTEROL_SAVE = "IV_Cholesterol_Save"
    /** 从心率录入新数据界面点击保存数据时 */
    val IV_HEART_RATE_SAVE = "IV_HeartRate_Save"
    /** 从体重BMI录入新数据界面点击保存数据时 */
    val IV_BMI_SAVE = "IV_BMI_Save"
    /** 保存步数目标 */
    val IV_STEP_GOAL_SAVE = "IV_Step_Goal_Save"

    // ==================== 插屏广告 - 药物提醒 ====================
    /** 从添加药物提醒界面返回时 */
    val IV_ADD_MEDS_BACK = "IV_AddMeds_back"
    /** 从添加药物提醒界面点击保存时 */
    val IV_ADD_MEDS_SAVE = "IV_AddMeds_Save"
    
    // ==================== 插屏广告 - 性别年龄选择页面返回 ====================
    val IV_PROFILE_BACK = "IV_Profile_back"
    // ==================== 插屏广告 - 详情/资讯页面返回 ====================
    /** 从新闻资讯详情界面返回时 */
    val IV_INSIGHTS_DETAILS_BACK = "IV_InsightsDetails_back"
    
    // ==================== 插屏广告 - 报表界面返回 ====================
    /** 从血糖报表界面返回时 */
    val IV_BLOOD_SUGAR_TRACK_BACK = "IV_BloodSugarTrack_back"
    /** 从血压报表界面返回时 */
    val IV_BLOOD_PRESSURE_TRACK_BACK = "IV_BloodPressureTrack_back"
    /** 从胆固醇血压报表界面返回时 */
    val IV_CHOLESTEROL_TRACK_BACK = "IV_CholesterolTrack_back"
    /** 从心率报表界面返回时 */
    val IV_HEART_RATE_TRACK_BACK = "IV_HeartRateTrack_back"
    /** 从体重BMI报表界面返回时 */
    val IV_BMI_TRACK_BACK = "IV_BMITrack_back"
    /** 从喝水报表界面返回时 */
    val IV_WATER_TRACK_BACK = "IV_WaterTrack_back"
    /** 从走路报表界面返回时 */
    val IV_WALK_TRACK_BACK = "IV_WalkTrack_back"
    
    // ==================== 插屏广告 - 进入报表界面 ====================
    /** 点击进入血糖报表界面时 */
    val IV_BLOOD_SUGAR_TRACK_ENTER = "IV_BloodSugarTrack_Enter"
    /** 点击进入血压报表界面时 */
    val IV_BLOOD_PRESSURE_TRACK_ENTER = "IV_BloodPressureTrack_Enter"
    /** 点击进入胆固醇血压报表界面时 */
    val IV_CHOLESTEROL_TRACK_ENTER = "IV_CholesterolTrack_Enter"
    /** 点击进入心率录入新数据界面时 */
    val IV_HEART_RATE_TRACK_ENTER = "IV_HeartRateTrack_Enter"
    /** 点击进入体重BMI报表界面时 */
    val IV_BMI_TRACK_ENTER = "IV_BMITrack_Enter"
    /** 点击进入喝水报表界面时 */
    val IV_WATER_TRACK_ENTER = "IV_WaterTrack_Enter"
    /** 点击进入走路报表界面时 */
    val IV_WALK_TRACK_ENTER = "IV_WalkTrack_Enter"
    
    // ==================== 激励广告 - 获取建议 ====================
    /** 血糖录入数据成功后点击获取建议 */
    val RV_BLOOD_SUGAR_NOTE = "RV_BloodSugar_Note"
    /** 血压录入数据成功后点击获取建议 */
    val RV_BLOOD_PRESSURE_NOTE = "RV_BloodPressure_Note"
    /** 胆固醇录入数据成功后点击获取建议 */
    val RV_CHOLESTEROL_NOTE = "RV_Cholesterol_Note"
    /** 心率录入数据成功后点击获取建议 */
    val RV_HEART_RATE_NOTE = "RV_HeartRate_Note"
    /** BMI录入数据成功后点击获取建议 */
    val RV_BMI_NOTE = "RV_BMI_Note"
    
    // ==================== 原生广告 - 首页Tab (Home) ====================
    /** 退出弹窗原生广告 */
    val NA_HOME_EXIT_DIALOG = "NA_Home_exit_dialog"
    
    // ==================== 原生广告 - Record Tab ====================
    /** Record Tab 中间广告 */
    val NA_MAIN_TRACKER_MIDDLE = "NA_Main_Tracker_middle"
    /** 饮水完成页底部广告 */
    val NA_HYDRATE_COMPLETE_BOTTOM = "NA_Hydrate_Complete_bottom"
    
    // ==================== 原生广告 - Meds Tab ====================
    /** 药物提醒列表广告 */
    val NA_MEDS_REMINDER_LIST = "NA_Meds_reminder_list"
    /** 闹钟管理页底部广告 */
    val NA_ALARM_MANAGER_BOTTOM = "NA_Alarm_Manager_bottom"
    
    // ==================== 原生广告 - Insights Tab ====================
    /** 资讯详情页底部广告 */
    val NA_INSIGHTS_DETAIL_BOTTOM = "NA_Insights_detail_bottom"
    
    // ==================== 原生广告 - Settings Tab ====================
    /** 个人资料页底部广告 */
    val NA_SETTINGS_PROFILE_BOTTOM = "NA_Settings_profile_bottom"

    /** 语言选择页底部广告 */
    val NA_SETTINGS_LANGUAGE_BOTTOM = "NA_Settings_language_bottom"
    
    // ==================== 原生广告 - 录入页面 (NewRecord) ====================
    /** 血糖录入页底部广告 */
    val NA_NEW_RECORD_BLOOD_SUGAR_BOTTOM = "NA_NewRecord_BloodSugar_bottom"
    /** 血压录入页底部广告 */
    val NA_NEW_RECORD_BLOOD_PRESSURE_BOTTOM = "NA_NewRecord_BloodPressure_bottom"
    /** 胆固醇录入页底部广告 */
    val NA_NEW_RECORD_CHOLESTEROL_BOTTOM = "NA_NewRecord_Cholesterol_bottom"
    /** 心率录入页底部广告 */
    val NA_NEW_RECORD_HEART_RATE_BOTTOM = "NA_NewRecord_HeartRate_bottom"
    /** BMI录入页底部广告 */
    val NA_NEW_RECORD_BMI_BOTTOM = "NA_NewRecord_BMI_bottom"
    
    // ==================== 原生广告 - 详情页面 (Detail) ====================
    /** 血糖详情页底部广告 */
    val NA_DETAIL_BLOOD_SUGAR_BOTTOM = "NA_Detail_BloodSugar_bottom"
    /** 血压详情页底部广告 */
    val NA_DETAIL_BLOOD_PRESSURE_BOTTOM = "NA_Detail_BloodPressure_bottom"
    /** 胆固醇详情页底部广告 */
    val NA_DETAIL_CHOLESTEROL_BOTTOM = "NA_Detail_Cholesterol_bottom"
    /** 心率详情页底部广告 */
    val NA_DETAIL_HEART_RATE_BOTTOM = "NA_Detail_HeartRate_bottom"
    /** BMI详情页底部广告 */
    val NA_DETAIL_BMI_BOTTOM = "NA_Detail_BMI_bottom"
    /** 详情页弹窗广告(确认弹窗) */
    val NA_DETAIL_CONFIRM_DIALOG = "NA_Detail_confirm_dialog"
    
    // ==================== 原生广告 - 卸载拦截 ====================
    /** 卸载拦截页1底部广告 */
    val NA_UNINSTALL_1_BOTTOM = "NA_Uninstall1_bottom"
    /** 卸载拦截页2底部广告 */
    val NA_UNINSTALL_2_BOTTOM = "NA_Uninstall2_bottom"


    val IV_UNINSTALL_1 = "IV_Uninstall1"
    val IV_UNINSTALL_2 = "IV_Uninstall2"
    // ==================== 原生广告 - 其他 ====================
    /** 地震详情页底部广告 */
    val NA_EARTHQUAKE_BOTTOM = "NA_Earthquake_bottom"
    
    // ==================== 横幅广告 ====================
    /** 首页底部横幅广告 */
    val BA_HOME_BOTTOM = "BA_Home_bottom"
    val NA_NEW_GUIDE_FULL = "NA_New_Guide_Full"
}

# 广告位 Position 定义文档

## 测试验证说明

### 日志过滤命令
```bash
adb logcat -s AdPosition
```

### 日志输出格式
```
D/AdPosition: Event: {事件名}, Position: {PositionName}
```

### 验证示例
```
D/AdPosition: Event: ad_position, Position: IV_BloodSugar_Save
D/AdPosition: Event: ad_impression, Position: NA_Home_exit_dialog
D/AdPosition: Event: ad_click, Position: BA_Home_bottom
```

### 事件类型说明
| 事件名 | 说明 |
|--------|------|
| ad_position | 广告位触发 |
| ad_impression | 广告展示成功 |
| ad_click | 广告被点击 |
| ad_revenue | 广告收入上报 |

---

## 广告位定义

| 广告类型 | 广告位置 | PositionName |
|----------|----------|--------------|
| 开屏 | APP冷热启动时 | SP_AppStart |
| 插屏 | 从血糖录入新数据界面返回时 | IV_BloodSugar_back |
| 插屏 | 从血压录入新数据界面返回时 | IV_BloodPressure_back |
| 插屏 | 从胆固醇录入新数据界面返回时 | IV_Cholesterol_back |
| 插屏 | 从心率录入新数据界面返回时 | IV_HeartRate_back |
| 插屏 | 从体重BMI录入新数据界面返回时 | IV_BMI_back |
| 插屏 | 从喝水录入新数据界面返回时 | IV_Water_back |
| 插屏 | 从走路录入新数据界面返回时 | IV_Walk_back |
| 插屏 | 从血糖录入新数据界面点击保存数据时 | IV_BloodSugar_Save |
| 插屏 | 从血压录入新数据界面点击保存数据时 | IV_BloodPressure_Save |
| 插屏 | 从胆固醇录入新数据界面点击保存数据时 | IV_Cholesterol_Save |
| 插屏 | 从心率录入新数据界面点击保存数据时 | IV_HeartRate_Save |
| 插屏 | 从体重BMI录入新数据界面点击保存数据时 | IV_BMI_Save |
| 插屏 | 从添加药物提醒界面返回时 | IV_AddMeds_back |
| 插屏 | 从添加药物提醒界面点击保存时 | IV_AddMeds_Save |
| 插屏 | 从个人资料页返回时 | IV_Profile_back |
| 插屏 | 从新闻资讯详情界面返回时 | IV_InsightsDetails_back |
| 插屏 | 从血糖报表界面返回时 | IV_BloodSugarTrack_back |
| 插屏 | 从血压报表界面返回时 | IV_BloodPressureTrack_back |
| 插屏 | 从胆固醇报表界面返回时 | IV_CholesterolTrack_back |
| 插屏 | 从心率报表界面返回时 | IV_HeartRateTrack_back |
| 插屏 | 从体重BMI报表界面返回时 | IV_BMITrack_back |
| 插屏 | 从喝水报表界面返回时 | IV_WaterTrack_back |
| 插屏 | 从走路报表界面返回时 | IV_WalkTrack_back |
| 插屏 | 点击进入血糖报表界面时 | IV_BloodSugarTrack_Enter |
| 插屏 | 点击进入血压报表界面时 | IV_BloodPressureTrack_Enter |
| 插屏 | 点击进入胆固醇报表界面时 | IV_CholesterolTrack_Enter |
| 插屏 | 点击进入心率报表界面时 | IV_HeartRateTrack_Enter |
| 插屏 | 点击进入体重BMI报表界面时 | IV_BMITrack_Enter |
| 插屏 | 点击进入喝水报表界面时 | IV_WaterTrack_Enter |
| 插屏 | 点击进入走路报表界面时 | IV_WalkTrack_Enter |
| 插屏 | 卸载拦截页1点击继续卸载时 | IV_Uninstall1 |
| 插屏 | 卸载拦截页2点击继续卸载时 | IV_Uninstall2 |
| 激励 | 血糖录入数据成功后点击获取建议 | RV_BloodSugar_Note |
| 激励 | 血压录入数据成功后点击获取建议 | RV_BloodPressure_Note |
| 激励 | 胆固醇录入数据成功后点击获取建议 | RV_Cholesterol_Note |
| 激励 | 心率录入数据成功后点击获取建议 | RV_HeartRate_Note |
| 激励 | BMI录入数据成功后点击获取建议 | RV_BMI_Note |
| 原生 | 退出弹窗原生广告 | NA_Home_exit_dialog |
| 原生 | Record Tab 中间广告 | NA_Main_Tracker_middle |
| 原生 | 饮水完成页底部广告 | NA_Hydrate_Complete_bottom |
| 原生 | 药物提醒列表广告 | NA_Meds_reminder_list |
| 原生 | 闹钟管理页底部广告 | NA_Alarm_Manager_bottom |
| 原生 | 资讯详情页底部广告 | NA_Insights_detail_bottom |
| 原生 | 个人资料页底部广告 | NA_Settings_profile_bottom |
| 原生 | 语言选择页底部广告 | NA_Settings_language_bottom |
| 原生 | 血糖录入页底部广告 | NA_NewRecord_BloodSugar_bottom |
| 原生 | 血压录入页底部广告 | NA_NewRecord_BloodPressure_bottom |
| 原生 | 胆固醇录入页底部广告 | NA_NewRecord_Cholesterol_bottom |
| 原生 | 心率录入页底部广告 | NA_NewRecord_HeartRate_bottom |
| 原生 | BMI录入页底部广告 | NA_NewRecord_BMI_bottom |
| 原生 | 血糖详情页底部广告 | NA_Detail_BloodSugar_bottom |
| 原生 | 血压详情页底部广告 | NA_Detail_BloodPressure_bottom |
| 原生 | 胆固醇详情页底部广告 | NA_Detail_Cholesterol_bottom |
| 原生 | 心率详情页底部广告 | NA_Detail_HeartRate_bottom |
| 原生 | BMI详情页底部广告 | NA_Detail_BMI_bottom |
| 原生 | 详情页弹窗广告(确认弹窗) | NA_Detail_confirm_dialog |
| 原生 | 卸载拦截页1底部广告 | NA_Uninstall1_bottom |
| 原生 | 卸载拦截页2底部广告 | NA_Uninstall2_bottom |
| 原生 | 地震详情页底部广告 | NA_Earthquake_bottom |
| 原生 | 新手引导全屏原生广告 | NA_New_Guide_Full |
| 横幅 | 首页底部横幅广告 | BA_Home_bottom |


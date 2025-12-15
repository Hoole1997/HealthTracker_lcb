package com.android.common.weather.model

import com.google.gson.annotations.SerializedName

/**
 * AccuWeather API 完整数据模型
 * 
 * 基于 AccuWeather 官方 API 文档定义的 Kotlin 数据类
 * 参考：https://developer.accuweather.com/
 * 
 * 注意：部分类名与项目现有类冲突，已添加 "AccuWeather" 前缀以区分
 */

// ==================== 基础数据类型 ====================

/**
 * 国家或地区信息
 */
data class RegionOrCountry(
    @SerializedName("ID") val id: String? = null,                          // 地区/国家唯一标识
    @SerializedName("LocalizedName") val localizedName: String? = null,    // 本地化名称
    @SerializedName("EnglishName") val englishName: String? = null         // 英文名称
)

/**
 * 时区信息
 */
data class TimeZone(
    @SerializedName("Code") val code: String? = null,                      // 时区代码（如 CST）
    @SerializedName("NextOffsetChange") val nextOffsetChange: String? = null, // 下次时区偏移变更时间
    @SerializedName("Name") val name: String?= null,                      // 时区名称
    @SerializedName("GmtOffset") val gmtOffset: Float? = null,             // GMT 偏移量（小时）
    @SerializedName("IsDaylightSaving") val isDaylightSaving: Boolean? = null // 是否夏令时
)

/**
 * 地理位置信息
 */
data class GeoPosition(
    @SerializedName("Elevation") val elevation: Units? = null,  // 海拔高度（公制/英制）
    @SerializedName("Latitude") val latitude: Float? = null,    // 纬度
    @SerializedName("Longitude") val longitude: Float? = null   // 经度
)

/**
 * 行政区划信息
 */
data class AdministrativeArea(
    @SerializedName("Level") val level: Int? = null,                        // 行政等级
    @SerializedName("CountryId") val countryId: String? = null,            // 所属国家 ID
    @SerializedName("ID") val id: String? = null,                          // 行政区 ID
    @SerializedName("LocalizedName") val localizedName: String? = null,    // 本地化名称
    @SerializedName("EnglishName") val englishName: String? = null,        // 英文名称
    @SerializedName("LocalizedType") val localizedType: String? = null,    // 本地化行政区类型（如"省"）
    @SerializedName("EnglishType") val englishType: String? = null         // 英文行政区类型（如"Province"）
)

/**
 * 单位信息（温度、距离、速度等单一度量值）
 */
data class UnitInfo(
    @SerializedName("Value") val value: Float? = null,        // 具体数值
    @SerializedName("Unit") val unit: String? = null,         // 单位字符串（如 "C"、"F"、"m"、"ft"）
    @SerializedName("UnitType") val unitType: Int? = null,    // 单位类型代码（17=摄氏度, 18=华氏度, 5=米, 0=英尺）
    @SerializedName("Phrase") val phrase: String? = null      // 描述短语（如体感温度的描述）
)

/**
 * 公制和英制双单位值
 * 用于同时提供公制（国际单位制）和英制（美国习惯单位）的测量值
 */
data class Units(
    @SerializedName("Metric") val metric: UnitInfo? = null,     // 公制单位值（摄氏度、米、公里/小时等）
    @SerializedName("Imperial") val imperial: UnitInfo? = null  // 英制单位值（华氏度、英尺、英里/小时等）
)

/**
 * 温度范围（用于每日预报）
 * 直接包含 Value/Unit/UnitType，不区分公制英制
 */
data class TemperatureRange(
    @SerializedName("Minimum") val minimum: UnitInfo? = null,  // 最小值
    @SerializedName("Maximum") val maximum: UnitInfo? = null   // 最大值
)

/**
 * 单位值范围（带公制/英制，用于温度汇总等）
 */
data class UnitsRange(
    @SerializedName("Minimum") val minimum: Units? = null,  // 最小值
    @SerializedName("Maximum") val maximum: Units? = null,  // 最大值
    @SerializedName("Average") val average: Units? = null   // 平均值（部分 API 不返回）
)

/**
 * 数值范围（纯数字）
 */
data class NumbersRange(
    @SerializedName("Minimum") val minimum: Float? = null,  // 最小值
    @SerializedName("Maximum") val maximum: Float? = null,  // 最大值
    @SerializedName("Average") val average: Float? = null   // 平均值（部分 API 不返回）
)

/**
 * 风向信息
 */
data class Direction(
    @SerializedName("Degrees") val degrees: Int? = null,        // 风向角度（以北为0°，顺时针，180°表示南风）
    @SerializedName("Localized") val localized: String? = null, // 本地化风向缩写（如"北风"）
    @SerializedName("English") val english: String? = null      // 英文风向缩写（如"N"、"NE"）
)

/**
 * 风信息
 */
data class Wind(
    @SerializedName("Direction") val direction: Direction? = null, // 风向
    @SerializedName("Speed") val speed: Units? = null              // 风速（公制/英制）
)

/**
 * 气压变化趋势
 */
data class PressureTendency(
    @SerializedName("LocalizedText") val localizedText: String? = null, // 本地化描述（如"上升"、"下降"）
    @SerializedName("Code") val code: String? = null                    // 趋势代码
)

/**
 * 降水量汇总（不同时间段的累计降水量）
 */
data class PrecipitationSummary(
    @SerializedName("Precipitation") val precipitation: Units? = null,  // 总降水量
    @SerializedName("PastHour") val pastHour: Units? = null,        // 过去1小时降水量
    @SerializedName("Past3Hours") val past3Hours: Units? = null,    // 过去3小时降水量
    @SerializedName("Past6Hours") val past6Hours: Units? = null,    // 过去6小时降水量
    @SerializedName("Past9Hours") val past9Hours: Units? = null,    // 过去9小时降水量
    @SerializedName("Past12Hours") val past12Hours: Units? = null,  // 过去12小时降水量
    @SerializedName("Past18Hours") val past18Hours: Units? = null,  // 过去18小时降水量
    @SerializedName("Past24Hours") val past24Hours: Units? = null   // 过去24小时降水量
)

/**
 * 温度信息汇总（不同时间段的温度范围）
 */
data class TemperatureSummary(
    @SerializedName("Past6HourRange") val past6HourRange: UnitsRange? = null,   // 过去6小时温度范围
    @SerializedName("Past12HourRange") val past12HourRange: UnitsRange? = null, // 过去12小时温度范围
    @SerializedName("Past24HourRange") val past24HourRange: UnitsRange? = null  // 过去24小时温度范围
)

/**
 * 太阳和月亮信息
 */
data class SunAndMoon(
    @SerializedName("Rise") val rise: String? = null,           // 升起时间（ISO8601 格式：yyyy-mm-ddThh:mm:ss±hh:mm）
    @SerializedName("EpochRise") val epochRise: Long? = null,   // 升起时间（Unix 时间戳，自1970年1月1日起的秒数）
    @SerializedName("Set") val set: String? = null,             // 落下时间（ISO8601 格式）
    @SerializedName("EpochSet") val epochSet: Long? = null,     // 落下时间（Unix 时间戳）
    @SerializedName("Phase") val phase: String? = null,         // 月相（仅月亮有，如"新月"、"满月"）
    @SerializedName("Age") val age: Int? = null                 // 距新月天数（仅月亮有）
)

/**
 * 度日汇总（供暖度日/制冷度日）
 * 用于计算建筑物供暖或制冷需求的指标
 */
data class DegreeDaySummary(
    @SerializedName("Heating") val heating: UnitInfo? = null, // 供暖度日（平均温度低于65°F的度数）
    @SerializedName("Cooling") val cooling: UnitInfo? = null  // 制冷度日（平均温度高于65°F的度数）
)

/**
 * 空气质量和花粉信息
 */
data class AirAndPollen(
    @SerializedName("Name") val name: String? = null,               // 类型名称（grass=草花粉、mold=霉菌、weed=杂草花粉、tree=树花粉、air quality=空气质量、UV=紫外线）
    @SerializedName("Value") val value: Int? = null,                // 数值（花粉单位：个/立方米；空气质量和UV：指数，无单位）
    @SerializedName("Category") val category: String? = null,       // 类别（low=低、high=高、good=良好、moderate=中等、unhealthy=不健康、hazardous=危险）
    @SerializedName("CategoryValue") val categoryValue: Int? = null, // 类别值（1=良好，6=危险）
    @SerializedName("Type") val type: String? = null                // 污染物类型（仅空气质量，如 ozone=臭氧、particle pollution=颗粒物污染）
)

/**
 * 预报总览/头条信息
 */
data class Headline(
    @SerializedName("EffectiveDate") val effectiveDate: String? = null,         // 生效日期（ISO8601 格式）
    @SerializedName("EffectiveEpochDate") val effectiveEpochDate: Long? = null, // 生效日期（Unix 时间戳）
    @SerializedName("Severity") val severity: Int? = null,                      // 严重程度（数字越小越严重）
    @SerializedName("Text") val text: String? = null,                           // 头条文本内容
    @SerializedName("Category") val category: String? = null,                   // 类别
    @SerializedName("EndDate") val endDate: String? = null,                     // 结束日期（ISO8601 格式）
    @SerializedName("EndEpochDate") val endEpochDate: Long? = null,            // 结束日期（Unix 时间戳）
    @SerializedName("MobileLink") val mobileLink: String? = null,              // 移动端详情链接
    @SerializedName("Link") val link: String? = null                           // Web端详情链接
)

// ==================== 天气信息类 ====================

/**
 * 天气详细信息（白天或夜间）
 * 包含完整的天气预报信息，如温度、降水、风力等
 */
data class WeatherInfo(
    @SerializedName("Icon") val icon: Int? = null,                              // 天气图标 ID（1-44）
    @SerializedName("LocalSource") val localSource: LocalSource? = null,        // 本地天气数据来源
    @SerializedName("IconPhrase") val iconPhrase: String? = null,              // 图标对应的短语描述
    @SerializedName("HasPrecipitation") val hasPrecipitation: Boolean? = null, // 是否有降水
    @SerializedName("PrecipitationType") val precipitationType: String? = null, // 降水类型（Rain=雨、Snow=雪、Ice=冰、Mixed=混合）
    @SerializedName("PrecipitationIntensity") val precipitationIntensity: String? = null, // 降水强度（Light=小、Moderate=中、Heavy=大）
    @SerializedName("ShortPhrase") val shortPhrase: String? = null,            // 简短描述（≤30字符）
    @SerializedName("LongPhrase") val longPhrase: String? = null,              // 详细描述（≤100字符）
    @SerializedName("PrecipitationProbability") val precipitationProbability: Int? = null, // 降水概率（0-100%）
    @SerializedName("ThunderstormProbability") val thunderstormProbability: Int? = null, // 雷暴概率（0-100%）
    @SerializedName("RainProbability") val rainProbability: Int? = null,       // 降雨概率（0-100%）
    @SerializedName("SnowProbability") val snowProbability: Int? = null,       // 降雪概率（0-100%）
    @SerializedName("IceProbability") val iceProbability: Int? = null,         // 冰雹概率（0-100%）
    @SerializedName("Wind") val wind: Wind? = null,                            // 风信息
    @SerializedName("WindGust") val windGust: Wind? = null,                    // 阵风信息
    @SerializedName("TotalLiquid") val totalLiquid: UnitInfo? = null,          // 12小时总液态降水量
    @SerializedName("Rain") val rain: UnitInfo? = null,                        // 12小时总降雨量
    @SerializedName("Snow") val snow: UnitInfo? = null,                        // 12小时总降雪量
    @SerializedName("Ice") val ice: UnitInfo? = null,                          // 12小时冰雹总量
    @SerializedName("HoursOfPrecipitation") val hoursOfPrecipitation: Float? = null, // 12小时内降水持续小时数
    @SerializedName("HoursOfRain") val hoursOfRain: Float? = null,            // 12小时内降雨持续小时数
    @SerializedName("HoursOfSnow") val hoursOfSnow: Float? = null,            // 12小时内降雪持续小时数
    @SerializedName("HoursOfIce") val hoursOfIce: Float? = null,              // 12小时内冰雹持续小时数
    @SerializedName("CloudCover") val cloudCover: Int? = null,                // 云量百分比（0-100%）
    @SerializedName("Evapotranspiration") val evapotranspiration: UnitInfo? = null, // 蒸散量
    @SerializedName("SolarIrradiance") val solarIrradiance: UnitInfo? = null, // 太阳辐照度
    @SerializedName("RelativeHumidity") val relativeHumidity: NumbersRange? = null, // 相对湿度范围（0-100%）
    @SerializedName("WetBulbTemperature") val wetBulbTemperature: UnitsRange? = null, // 湿球温度（在恒定压力下通过蒸发水使空气达到饱和的温度）
    @SerializedName("WetBulbGlobeTemperature") val wetBulbGlobeTemperature: UnitsRange? = null, // 湿球黑球温度（在阳光直射下人体热应激指标）
    @SerializedName("UVIndexFloat") val uvIndexFloat: NumbersRange? = null    // 紫外线指数范围（0-11+，浮点数）
)

/**
 * 每日天气预报（AccuWeather 版本）
 * 
 * 注意：为避免与项目现有的 DailyForecast 冲突，使用 AccuWeatherDailyForecast 命名
 */
data class AccuWeatherDailyForecast(
    @SerializedName("Date") val date: String? = null,                 // 预报日期（ISO8601 格式：yyyy-mm-ddThh:mm:ss±hh:mm）
    @SerializedName("EpochDate") val epochDate: Long? = null,         // 预报日期（Unix 时间戳）
    @SerializedName("Temperature") val temperature: TemperatureRange? = null, // 温度范围
    @SerializedName("Day") val day: WeatherInfo? = null,              // 白天天气详情
    @SerializedName("Night") val night: WeatherInfo? = null,          // 夜间天气详情
    @SerializedName("Sources") val sources: List<String>? = null,     // 预报来源列表
    @SerializedName("MobileLink") val mobileLink: String? = null,     // 移动端详情链接
    @SerializedName("Link") val link: String? = null,                 // Web端详情链接
    @SerializedName("Sun") val sun: SunAndMoon? = null,               // 太阳信息（日出日落）
    @SerializedName("Moon") val moon: SunAndMoon? = null,             // 月亮信息（月升月落、月相）
    @SerializedName("RealFeelTemperature") val realFeelTemperature: TemperatureRange? = null, // AccuWeather 体感温度（专利算法）
    @SerializedName("RealFeelTemperatureShade") val realFeelTemperatureShade: TemperatureRange? = null, // 阴影处体感温度
    @SerializedName("HoursOfSun") val hoursOfSun: Float? = null,      // 日照时长（小时）
    @SerializedName("DegreeDaySummary") val degreeDaySummary: DegreeDaySummary? = null, // 度日汇总
    @SerializedName("AirAndPollen") val airAndPollen: List<AirAndPollen>? = null // 空气质量和花粉列表
)

/**
 * 城市信息
 */
data class CityInfo(
    @SerializedName("PrimaryPostalCode") val primaryPostalCode: String? = null,  // 主要邮政编码
    @SerializedName("Region") val region: RegionOrCountry? = null,              // 地区信息
    @SerializedName("Country") val country: RegionOrCountry? = null,            // 国家信息
    @SerializedName("TimeZone") val timeZone: TimeZone? = null,                 // 时区信息
    @SerializedName("GeoPosition") val geoPosition: GeoPosition? = null,        // 地理位置坐标
    @SerializedName("IsAlias") val isAlias: Boolean? = null,                    // 是否为别名
    @SerializedName("SupplementalAdminAreas") val supplementalAdminAreas: List<AdministrativeArea>? = null, // 补充行政区划列表
    @SerializedName("DataSets") val dataSets: List<String>? = null,             // 可用数据集（如"AirQuality"、"MinuteCast"）
    @SerializedName("EnglishName") val englishName: String? = null,             // 英文名称
    @SerializedName("Version") val version: Int? = null,                        // API 版本号
    @SerializedName("Key") val key: String? = null,                             // 地区唯一 ID（用于查询天气）
    @SerializedName("Type") val type: String? = null,                           // 地点类型（City=城市、PostalCode=邮编、POI=兴趣点、LatLong=经纬度）
    @SerializedName("Rank") val rank: Int? = null,                              // 重要性排名（基于人口、政治重要性、地理大小等）
    @SerializedName("LocalizedName") val localizedName: String? = null,         // 本地化名称
    @SerializedName("AdministrativeArea") val administrativeArea: AdministrativeArea? = null // 所属行政区
)

/**
 * 本地天气来源信息
 */
data class LocalSource(
    @SerializedName("Id") val id: Int? = null,              // 来源 ID
    @SerializedName("Name") val name: String? = null,      // 来源名称（如"Huafeng"）
    @SerializedName("WeatherCode") val weatherCode: String? = null // 天气代码
)

// ==================== API 响应类 ====================

/**
 * 天气预报响应（多日预报）
 */
data class DayForecasts(
    @SerializedName("Headline") val headline: Headline? = null,                                    // 预报总览/头条
    @SerializedName("DailyForecasts") val dailyForecasts: List<AccuWeatherDailyForecast>? = null // 每日预报列表
)

/**
 * 当前天气状况响应（完整版）
 * 
 * 注意：为避免与 WeatherResponse.kt 中的简化版 CurrentConditions 冲突，
 * 使用 CurrentConditionResponse 命名
 */
data class CurrentConditionResponse(
    @SerializedName("LocalObservationDateTime") val localObservationDateTime: String? = null, // 本地观测时间（ISO8601 格式）
    @SerializedName("EpochTime") val epochTime: Long? = null,                                // 观测时间（Unix 时间戳）
    @SerializedName("WeatherText") val weatherText: String? = null,                          // 天气文本描述
    @SerializedName("WeatherIcon") val weatherIcon: Int? = null,                             // 天气图标 ID（1-44）
    @SerializedName("HasPrecipitation") val hasPrecipitation: Boolean? = null,              // 是否有降水
    @SerializedName("PrecipitationType") val precipitationType: String? = null,             // 降水类型
    @SerializedName("LocalSource") val localSource: LocalSource? = null,
    @SerializedName("IsDayTime") val isDayTime: Boolean? = null,                            // 是否白天
    @SerializedName("Temperature") val temperature: Units? = null,                          // 当前温度
    @SerializedName("RealFeelTemperature") val realFeelTemperature: Units? = null,          // AccuWeather 体感温度
    @SerializedName("RealFeelTemperatureShade") val realFeelTemperatureShade: Units? = null, // 阴影处体感温度
    @SerializedName("MobileLink") val mobileLink: String? = null,                           // 移动端详情链接
    @SerializedName("Link") val link: String? = null,                                       // Web端详情链接
    @SerializedName("RelativeHumidity") val relativeHumidity: Int? = null,                  // 相对湿度（%）
    @SerializedName("IndoorRelativeHumidity") val indoorRelativeHumidity: Int? = null,      // 室内相对湿度（%）
    @SerializedName("DewPoint") val dewPoint: Units? = null,                                // 露点温度
    @SerializedName("Wind") val wind: Wind? = null,                                         // 风信息
    @SerializedName("WindGust") val windGust: Wind? = null,                                 // 阵风信息
    @SerializedName("UVIndex") val uvIndex: Int? = null,                                    // 紫外线指数（整数）
    @SerializedName("UVIndexFloat") val uvIndexFloat: Float? = null,                        // 紫外线指数（浮点数）
    @SerializedName("UVIndexText") val uvIndexText: String? = null,                         // 紫外线强度描述
    @SerializedName("Visibility") val visibility: Units? = null,                            // 能见度
    @SerializedName("ObstructionsToVisibility") val obstructionsToVisibility: String? = null, // 能见度障碍物
    @SerializedName("CloudCover") val cloudCover: Int? = null,                              // 云量（%）
    @SerializedName("Ceiling") val ceiling: Units? = null,                                  // 云层高度
    @SerializedName("Pressure") val pressure: Units? = null,                                // 气压
    @SerializedName("PressureTendency") val pressureTendency: PressureTendency? = null,     // 气压变化趋势
    @SerializedName("Past24HourTemperatureDeparture") val past24HourTemperatureDeparture: Units? = null, // 过去24小时温度偏差
    @SerializedName("WindChillTemperature") val windChillTemperature: Units? = null,        // 风寒温度（考虑风速的体感温度）
    @SerializedName("WetBulbTemperature") val wetBulbTemperature: Units? = null,            // 湿球温度
    @SerializedName("WetBulbGlobeTemperature") val wetBulbGlobeTemperature: Units? = null,  // 湿球黑球温度
    @SerializedName("Precip1hr") val precip1hr: Units? = null,                              // 过去1小时降水量
    @SerializedName("PrecipitationSummary") val precipitationSummary: PrecipitationSummary? = null, // 降水量汇总
    @SerializedName("TemperatureSummary") val temperatureSummary: TemperatureSummary? = null, // 温度汇总
    @SerializedName("ApparentTemperature") val apparentTemperature: Units? = null           // 表观温度（综合温度、湿度、风速的体感温度）
)

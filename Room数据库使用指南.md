# Room数据库使用指南

## 概述

本项目使用Room数据库来管理血糖、血压健康数据和标签系统。Room是Android官方推荐的SQLite数据库抽象层，提供了编译时的SQL验证和类型安全的数据库操作。项目采用Hilt依赖注入来管理数据库实例和Repository。

## 项目结构

```
data/
├── entity/                     # 数据实体类
│   ├── BloodSugarRecord.kt    # 血糖记录实体
│   ├── BloodPressureRecord.kt # 血压记录实体
│   └── HealthTag.kt           # 健康标签实体
├── dao/                       # 数据访问对象
│   ├── BloodSugarDao.kt       # 血糖数据DAO
│   ├── BloodPressureDao.kt    # 血压数据DAO
│   └── HealthTagDao.kt        # 标签数据DAO
├── database/                  # 数据库配置
│   └── HealthDatabase.kt      # Room数据库类
├── converter/                 # 类型转换器
│   └── DateTimeConverter.kt   # Date类型转换器
├── utils/                     # 工具类
│   ├── DateTimeUtils.kt       # 时间处理工具
│   ├── TagUtils.kt            # 标签处理工具
│   └── HealthLocalizationUtils.kt # 健康数据国际化工具
├── enums/                     # 枚举定义
│   └── HealthEnums.kt         # 健康数据相关枚举
└── repository/                # 数据仓库层（通过Hilt注入）
    ├── BloodSugarRepository.kt
    ├── BloodPressureRepository.kt
    └── HealthTagRepository.kt
```

## 核心特性

### 1. 数据模型设计

**血糖记录 (BloodSugarRecord)**:
- ✅ 使用`Date`类型存储时间（API 24+兼容）
- ✅ 支持血糖值范围验证 (18.0~630.0 mg/dL)
- ✅ 提供mmol/L单位转换
- ✅ 基于枚举的血糖等级判断逻辑
- ✅ 支持多标签关联（tagIds字段）
- ✅ 预留3个扩展字段

**血压记录 (BloodPressureRecord)**:
- ✅ 记录收缩压、舒张压、脉搏
- ✅ 基于AHA标准的血压分类算法
- ✅ 提供平均动脉压(MAP)计算
- ✅ 脉压差计算
- ✅ 支持多标签关联（tagIds字段）
- ✅ 预留3个扩展字段

**健康标签 (HealthTag)**:
- ✅ 支持预定义和自定义标签
- ✅ 用简单标识符区分标签类型（0=自定义，1=预定义）
- ✅ 支持标签的增删查改操作

### 2. 时间处理优化

- 使用`Date`类型存储时间（兼容API 24+）
- 通过`DateTimeConverter`实现时间戳存储转换
- `DateTimeUtils`提供多滚轮时间选择器支持
- 支持年月日时分钟精度的时间创建和解析

### 3. 国际化支持

- 所有健康分类使用枚举和代码值，避免硬编码语言字符串
- `HealthEnums.kt`定义标准化的医学分类
- `HealthLocalizationUtils.kt`提供本地化显示支持

### 4. 标签系统

- 预定义标签：兴奋状态、沮丧、平静的、剧烈运动后、有氧运动后、无氧运动后、节食、酒后
- 自定义标签：用户可创建和删除个人标签
- 多选支持：每个记录可关联多个标签
- 简单关联：使用逗号分隔的ID字符串存储

## Hilt依赖注入配置

### 1. 数据库模块配置

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHealthDatabase(@ApplicationContext context: Context): HealthDatabase {
        return Room.databaseBuilder(
            context,
            HealthDatabase::class.java,
            "health_database"
        )
        .addTypeConverter(DateTimeConverter())
        .build()
    }

    @Provides
    fun provideBloodSugarDao(database: HealthDatabase): BloodSugarDao {
        return database.bloodSugarDao()
    }

    @Provides
    fun provideBloodPressureDao(database: HealthDatabase): BloodPressureDao {
        return database.bloodPressureDao()
    }

    @Provides
    fun provideHealthTagDao(database: HealthDatabase): HealthTagDao {
        return database.healthTagDao()
    }
}
```

### 2. Repository通过Hilt注入

```kotlin
@Singleton
class BloodSugarRepository @Inject constructor(
    private val bloodSugarDao: BloodSugarDao,
    private val healthTagDao: HealthTagDao
) {

    // 添加血糖记录（支持标签）
    suspend fun addBloodSugarRecord(
        glucoseValue: Double,
        measurementTag: String,
        selectedTime: Date,
        tagIds: List<Long>? = null
    ): Long {
        val record = BloodSugarRecord.create(
            recordTime = selectedTime,
            glucoseValue = glucoseValue,
            measurementTag = measurementTag,
            tagIds = tagIds
        )
        return bloodSugarDao.insert(record)
    }

    // 获取最近7天的血糖记录
    fun getRecentBloodSugarRecords(): Flow<List<BloodSugarRecord>> {
        val (startDate, endDate) = DateTimeUtils.getDateRange(DateTimeUtils.now(), 7)
        return bloodSugarDao.getRecordsByTimeRange(startDate, endDate)
    }

    // 获取记录关联的标签
    suspend fun getRecordTags(record: BloodSugarRecord): List<HealthTag> {
        val tagIds = record.getTagIdList()
        return if (tagIds.isNotEmpty()) {
            healthTagDao.getByIds(tagIds)
        } else {
            emptyList()
        }
    }

    // 按标签筛选记录
    suspend fun getRecordsByTag(tagId: Long): List<BloodSugarRecord> {
        return bloodSugarDao.getRecordsByTagId(tagId.toString())
    }
}

@Singleton
class BloodPressureRepository @Inject constructor(
    private val bloodPressureDao: BloodPressureDao,
    private val healthTagDao: HealthTagDao
) {

    // 添加血压记录（支持标签）
    suspend fun addBloodPressureRecord(
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        measurementTag: String,
        selectedTime: Date,
        tagIds: List<Long>? = null
    ): Long {
        val record = BloodPressureRecord.create(
            recordTime = selectedTime,
            systolicPressure = systolic,
            diastolicPressure = diastolic,
            pulseRate = pulse,
            measurementTag = measurementTag,
            tagIds = tagIds
        )
        return bloodPressureDao.insert(record)
    }

    // 获取血压统计信息
    suspend fun getBloodPressureStatistics(): BloodPressureStatistics? {
        return bloodPressureDao.getBloodPressureStatistics()
    }

    // 获取异常血压记录
    fun getAbnormalBloodPressureRecords(): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsBySystolicRange(140, 300)
    }
}

@Singleton
class HealthTagRepository @Inject constructor(
    private val healthTagDao: HealthTagDao
) {

    // 初始化预定义标签
    suspend fun initializePredefinedTags() {
        val existingCount = healthTagDao.getCustomTagCount()
        if (existingCount == 0) {
            val predefinedTags = HealthTag.createAllPredefinedTags()
            healthTagDao.insertAll(predefinedTags)
        }
    }

    // 获取所有标签
    fun getAllTags(): Flow<List<HealthTag>> {
        return healthTagDao.getAllTags()
    }

    // 创建自定义标签
    suspend fun createCustomTag(name: String): Long? {
        val cleanName = TagUtils.cleanTagName(name)
        return if (cleanName != null && TagUtils.isValidTagName(cleanName)) {
            if (!healthTagDao.isNameExists(cleanName)) {
                val tag = HealthTag.createCustom(cleanName)
                healthTagDao.insert(tag)
            } else {
                null // 标签名已存在
            }
        } else {
            null // 标签名无效
        }
    }

    // 删除自定义标签
    suspend fun deleteCustomTag(tagId: Long): Boolean {
        val tag = healthTagDao.getById(tagId)
        return if (tag?.isCustomTag() == true) {
            healthTagDao.deleteById(tagId)
            true
        } else {
            false // 不能删除预定义标签
        }
    }

    // 搜索标签
    suspend fun searchTags(keyword: String): List<HealthTag> {
        return healthTagDao.searchTags(keyword)
    }
}
```

## 在ViewModel中使用（通过Hilt注入）

```kotlin
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val healthTagRepository: HealthTagRepository
) : ViewModel() {

    // 血糖记录列表
    val bloodSugarRecords = bloodSugarRepository.getRecentBloodSugarRecords()
        .asLiveData()

    // 所有标签
    val allTags = healthTagRepository.getAllTags()
        .asLiveData()

    // 初始化数据
    init {
        viewModelScope.launch {
            healthTagRepository.initializePredefinedTags()
        }
    }

    // 添加血糖记录（支持标签）
    fun addBloodSugarRecord(
        glucose: Double,
        tag: String,
        selectedTags: List<Long>,
        year: Int, month: Int, day: Int, hour: Int, minute: Int
    ) {
        viewModelScope.launch {
            val recordTime = DateTimeUtils.createDate(year, month, day, hour, minute)
            bloodSugarRepository.addBloodSugarRecord(
                glucoseValue = glucose,
                measurementTag = tag,
                selectedTime = recordTime,
                tagIds = selectedTags
            )
        }
    }

    // 添加血压记录（支持标签）
    fun addBloodPressureRecord(
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        tag: String,
        selectedTags: List<Long>,
        year: Int, month: Int, day: Int, hour: Int, minute: Int
    ) {
        viewModelScope.launch {
            val recordTime = DateTimeUtils.createDate(year, month, day, hour, minute)
            bloodPressureRepository.addBloodPressureRecord(
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                measurementTag = tag,
                selectedTime = recordTime,
                tagIds = selectedTags
            )
        }
    }

    // 创建自定义标签
    fun createCustomTag(name: String) {
        viewModelScope.launch {
            healthTagRepository.createCustomTag(name)
        }
    }

    // 删除自定义标签
    fun deleteCustomTag(tagId: Long) {
        viewModelScope.launch {
            healthTagRepository.deleteCustomTag(tagId)
        }
    }
}
```

## 多滚轮时间选择器集成

```kotlin
// 时间选择器回调
class TimePickerHelper {

    fun setupTimePicker(
        context: Context,
        defaultDate: Date = DateTimeUtils.now(),
        onTimeSelected: (Date) -> Unit
    ) {
        // 提取默认时间组件
        val components = DateTimeUtils.extractDateComponents(defaultDate)

        // 设置滚轮选择器
        val timePicker = MultiWheelTimePicker(context)
        timePicker.setDefaultTime(
            components.year,
            components.month,
            components.day,
            components.hour,
            components.minute
        )

        // 时间选择回调
        timePicker.setOnTimeSelectedListener { year, month, day, hour, minute ->
            val selectedDate = DateTimeUtils.createDate(year, month, day, hour, minute)
            onTimeSelected(selectedDate)
        }
    }
}
```

## 标签使用示例

```kotlin
// 在UI中显示标签
class HealthRecordAdapter {

    fun bindRecord(record: BloodSugarRecord, tags: List<HealthTag>) {
        // 显示血糖值
        glucoseValueText.text = "${record.glucoseValue} mg/dL"

        // 显示标签
        val tagDisplayText = TagUtils.getTagDisplayText(tags, maxDisplay = 3)
        tagsText.text = tagDisplayText
        tagsText.isVisible = tagDisplayText.isNotEmpty()

        // 设置血糖等级颜色
        val level = record.getGlucoseLevelEnum()
        val colorResource = HealthLocalizationUtils.getGlucoseLevelColorResource(level)
        // 应用颜色...
    }
}

// 标签选择器
class TagSelectorDialog {

    fun showTagSelector(
        context: Context,
        availableTags: List<HealthTag>,
        selectedTagIds: List<Long>,
        onTagsSelected: (List<Long>) -> Unit
    ) {
        // 分组显示标签
        val groupedTags = TagUtils.groupTagsByType(availableTags)
        val predefinedTags = groupedTags[true] ?: emptyList()
        val customTags = groupedTags[false] ?: emptyList()

        // 创建多选对话框...
        // 实现标签选择逻辑...
    }
}
```

## 数据库迁移

当需要升级数据库时，请按以下步骤操作：

### 1. 更新版本号

```kotlin
@Database(
    entities = [
        BloodSugarRecord::class,
        BloodPressureRecord::class,
        HealthTag::class
    ],
    version = 2, // 增加版本号
    exportSchema = true
)
```

### 2. 添加迁移策略

```kotlin
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加标签表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS health_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                is_predefined INTEGER NOT NULL,
                create_time INTEGER NOT NULL
            )
        """)

        // 为记录表添加标签字段
        database.execSQL("ALTER TABLE blood_sugar_records ADD COLUMN tag_ids TEXT")
        database.execSQL("ALTER TABLE blood_pressure_records ADD COLUMN tag_ids TEXT")
    }
}

// 在Hilt模块中添加迁移
@Provides
@Singleton
fun provideHealthDatabase(@ApplicationContext context: Context): HealthDatabase {
    return Room.databaseBuilder(context, HealthDatabase::class.java, "health_database")
        .addMigrations(MIGRATION_1_2)
        .addTypeConverter(DateTimeConverter())
        .build()
}
```

## 性能优化建议

### 1. 查询优化
- 使用索引加速常用查询字段（record_time, tag_ids等）
- 避免在UI线程执行数据库操作
- 使用分页查询处理大量数据
- 合理使用Flow来响应数据变化

### 2. 内存管理
- 及时取消不用的Flow订阅（viewModelScope会自动处理）
- 避免在循环中执行数据库查询
- 使用批量操作处理大量数据（insertAll等）

### 3. 数据完整性
- 使用事务处理相关操作
- 定期备份重要数据
- 实现数据验证逻辑（TagUtils.isValidTagName等）

## 测试建议

```kotlin
@HiltAndroidTest
class HealthDatabaseTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var database: HealthDatabase

    @Inject
    lateinit var bloodSugarDao: BloodSugarDao

    @Inject
    lateinit var healthTagDao: HealthTagDao

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun insertAndGetBloodSugarRecordWithTags() = runTest {
        // 创建标签
        val tag = HealthTag.createCustom("测试标签")
        val tagId = healthTagDao.insert(tag)

        // 创建记录
        val record = BloodSugarRecord.create(
            recordTime = DateTimeUtils.now(),
            glucoseValue = 120.0,
            measurementTag = "fasting",
            tagIds = listOf(tagId)
        )

        val recordId = bloodSugarDao.insert(record)
        val retrieved = bloodSugarDao.getById(recordId)

        assertThat(retrieved?.glucoseValue).isEqualTo(120.0)
        assertThat(retrieved?.hasTag(tagId)).isTrue()
    }
}
```

## 注意事项

1. **时间处理**: 使用Date类型确保API 24+兼容，统一使用DateTimeUtils工具类
2. **标签管理**: 预定义标签不可删除，自定义标签名称需要验证
3. **国际化**: 使用枚举代码值而非硬编码字符串，通过HealthLocalizationUtils获取显示文本
4. **Hilt注入**: 所有DAO和Repository都通过Hilt管理，确保单例和依赖关系正确
5. **数据验证**: 在Repository层实现业务逻辑验证，确保数据完整性
6. **扩展字段**: 合理使用预留的ext1、ext2、ext3字段，避免频繁修改表结构

通过以上设计，你的健康数据管理系统将具备良好的扩展性、性能和可维护性，同时支持完整的标签系统和国际化功能。
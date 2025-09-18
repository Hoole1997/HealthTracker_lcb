# BloodSugarRulerView 使用文档

## 概述

`BloodSugarRulerView` 是一个专为血糖值选择而设计的 Kotlin 自定义控件，基于原有的 `RulerView.java` 重新实现。支持 Float 类型刻度、可配置的刻度步长、范围限制以及丰富的自定义选项。

## 主要特性

### 核心功能
- ✅ **Float 类型支持** - 原生支持浮点数刻度值
- ✅ **可配置刻度步长** - 支持 0.1、1.0 等不同精度
- ✅ **范围限制** - 可设置可滚动范围，超出范围无法滚动
- ✅ **惯性滚动** - 支持惯性滚动并自动对齐到最近刻度
- ✅ **小数点控制** - 可配置小数点显示位数
- ✅ **无内部结果显示** - 控件专注于刻度选择，不显示当前值

### 视觉定制
- ✅ **刻度线高度** - 可分别设置小、中、大刻度线高度
- ✅ **指示线高度** - 可自定义中央指示线高度
- ✅ **颜色定制** - 支持各种颜色自定义
- ✅ **线条粗细** - 可设置各种线条的粗细
- ✅ **间距控制** - 可设置刻度值与刻度线的间距
- ✅ **内边距配置** - 可设置控件的水平和垂直内边距

### 血糖应用场景
- **mmol/L 模式**：范围 1.0-30.0，步长 0.1
- **mg/dL 模式**：范围 10-640，步长 1

## 使用方法

### 1. XML 布局中使用

#### mmol/L 血糖单位示例：
```xml
<com.healthtracker.blood.suger.ui.weight.BloodSugarRulerView
    android:id="@+id/bloodSugarRuler"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:minScale="0.0"
    app:maxScale="36.0"
    app:scrollableMinScale="1.0"
    app:scrollableMaxScale="30.0"
    app:scaleStep="0.1"
    app:firstScale="5.5"
    app:decimalPlaces="1"
    app:rulerHeight="80dp"
    app:scaleGap="15dp"
    app:scaleCount="10"
    app:smallScaleHeight="15dp"
    app:midScaleHeight="25dp"
    app:largeScaleHeight="35dp"
    app:indicatorHeight="60dp"
    app:smallScaleStroke="1dp"
    app:midScaleStroke="2dp"
    app:largeScaleStroke="3dp"
    app:indicatorStroke="3dp"
    app:smallScaleColor="#CCCCCC"
    app:midScaleColor="#999999"
    app:largeScaleColor="#666666"
    app:indicatorColor="#FF4081"
    app:scaleNumColor="#333333"
    app:scaleNumTextSize="14sp"
    app:scaleTextMargin="8dp"
    app:rulerPaddingHorizontal="16dp"
    app:rulerPaddingVertical="12dp"
    app:bgColor="#FFFFFF"
    app:isBgRoundRect="true" />
```

#### mg/dL 血糖单位示例：
```xml
<com.healthtracker.blood.suger.ui.weight.BloodSugarRulerView
    android:id="@+id/bloodSugarRulerMgDl"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:minScale="0"
    app:maxScale="700"
    app:scrollableMinScale="10"
    app:scrollableMaxScale="640"
    app:scaleStep="1.0"
    app:firstScale="100"
    app:decimalPlaces="0"
    app:rulerHeight="80dp"
    app:scaleGap="8dp"
    app:scaleCount="10"
    app:scaleTextMargin="6dp"
    app:rulerPaddingHorizontal="12dp"
    app:rulerPaddingVertical="8dp" />
```

### 2. 代码中使用

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var bloodSugarRuler: BloodSugarRulerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bloodSugarRuler = findViewById(R.id.bloodSugarRuler)

        // 设置监听器
        bloodSugarRuler.setOnChooseResultListener(object : BloodSugarRulerView.OnChooseResultListener {
            override fun onEndResult(result: String) {
                // 滚动结束时的最终结果
                Log.d("BloodSugar", "Final result: $result")
            }

            override fun onScrollResult(result: String) {
                // 滚动过程中的实时结果
                Log.d("BloodSugar", "Scrolling result: $result")
            }
        })

        // 设置血糖值范围 (mmol/L)
        bloodSugarRuler.setScaleRange(0f, 36f)
        bloodSugarRuler.setScrollableRange(1f, 30f)
        bloodSugarRuler.setScaleStep(0.1f)
        bloodSugarRuler.setDecimalPlaces(1)

        // 滚动到指定值
        bloodSugarRuler.scrollToScale(5.5f)

        // 获取当前值
        val currentValue = bloodSugarRuler.getCurrentScale()
    }
}
```

## 自定义属性详解

### 基础配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `scaleStep` | float | 0.1 | 刻度步长 |
| `minScale` | float | 0.0 | 最小刻度值 |
| `maxScale` | float | 100.0 | 最大刻度值 |
| `scrollableMinScale` | float | minScale | 可滚动的最小值 |
| `scrollableMaxScale` | float | maxScale | 可滚动的最大值 |
| `firstScale` | float | 50.0 | 初始显示的刻度值 |
| `decimalPlaces` | integer | 1 | 小数点位数 |

### 尺寸配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `rulerHeight` | dimension | 50dp | 尺子总高度 |
| `scaleGap` | dimension | 20dp | 刻度间距 |
| `scaleCount` | integer | 10 | 大刻度之间的小刻度数量 |
| `smallScaleHeight` | dimension | rulerHeight/4 | 小刻度高度 |
| `midScaleHeight` | dimension | rulerHeight/2 | 中刻度高度 |
| `largeScaleHeight` | dimension | rulerHeight/2+5 | 大刻度高度 |
| `indicatorHeight` | dimension | rulerHeight | 指示线高度 |

### 间距和内边距配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `scaleTextMargin` | dimension | 8dp | 刻度值与刻度线的间距 |
| `rulerPaddingHorizontal` | dimension | 0dp | 尺子水平内边距 |
| `rulerPaddingVertical` | dimension | 0dp | 尺子垂直内边距 |

### 线条配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `smallScaleStroke` | dimension | 1dp | 小刻度线粗细 |
| `midScaleStroke` | dimension | 2dp | 中刻度线粗细 |
| `largeScaleStroke` | dimension | 3dp | 大刻度线粗细 |
| `indicatorStroke` | dimension | 3dp | 指示线粗细 |

### 颜色配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `bgColor` | color | #FFFCFFFC | 背景色 |
| `smallScaleColor` | color | #FF999999 | 小刻度线颜色 |
| `midScaleColor` | color | #FF666666 | 中刻度线颜色 |
| `largeScaleColor` | color | #FF50B586 | 大刻度线颜色 |
| `indicatorColor` | color | #FF50B586 | 指示线颜色 |
| `scaleNumColor` | color | #FF333333 | 刻度数字颜色 |

### 文字配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `scaleNumTextSize` | dimension | 16sp | 刻度数字字体大小 |

### 其他配置
| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `isBgRoundRect` | boolean | true | 背景是否使用圆角 |

## API 方法

### 设置监听器
```kotlin
fun setOnChooseResultListener(listener: OnChooseResultListener)
```

### 滚动控制
```kotlin
fun scrollToScale(scale: Float) // 滚动到指定刻度值
fun getCurrentScale(): Float    // 获取当前刻度值
```

### 范围设置
```kotlin
fun setScaleRange(min: Float, max: Float)           // 设置刻度范围
fun setScrollableRange(min: Float, max: Float)      // 设置可滚动范围
```

### 精度设置
```kotlin
fun setScaleStep(step: Float)        // 设置刻度步长
fun setDecimalPlaces(places: Int)    // 设置小数点位数
```

## 回调接口

```kotlin
interface OnChooseResultListener {
    fun onEndResult(result: String)      // 滚动结束时回调
    fun onScrollResult(result: String)   // 滚动过程中回调
}
```

## 使用场景示例

### 1. mmol/L 血糖值选择
```kotlin
// 配置 mmol/L 模式
bloodSugarRuler.apply {
    setScaleRange(0f, 36f)
    setScrollableRange(1f, 30f)
    setScaleStep(0.1f)
    setDecimalPlaces(1)
    scrollToScale(5.5f) // 正常血糖值
}
```

### 2. mg/dL 血糖值选择
```kotlin
// 配置 mg/dL 模式
bloodSugarRuler.apply {
    setScaleRange(0f, 700f)
    setScrollableRange(10f, 640f)
    setScaleStep(1f)
    setDecimalPlaces(0)
    scrollToScale(100f) // 正常血糖值
}
```

## 注意事项

1. **范围限制**：`scrollableMinScale` 和 `scrollableMaxScale` 会限制用户的滚动范围，即使 `minScale` 和 `maxScale` 设置得更大也无法滚动到超出可滚动范围的值。

2. **刻度步长**：`scaleStep` 决定了相邻两个刻度值的差值，建议根据实际需求设置合理的步长。

3. **小数位数**：`decimalPlaces` 只影响显示格式，不影响实际的刻度计算精度。

4. **性能优化**：控件已经优化了绘制性能，只绘制屏幕可见的刻度，适合大范围的数值选择。

5. **内存管理**：控件会自动处理 VelocityTracker 和 ValueAnimator 的生命周期，无需手动释放。
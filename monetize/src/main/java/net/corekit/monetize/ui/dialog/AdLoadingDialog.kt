package net.corekit.monetize.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import net.corekit.monetize.R

/**
 * 全屏Loading弹框
 * 提供show和hide伴生对象函数
 * show时不允许关闭，只能通过hide关闭
 * 完全防止点击穿透
 */
class ADLoadingDialog private constructor(context: Context) : Dialog(context) {

    init {
        setupDialog()
    }

    private fun setupDialog() {
        // 设置无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        // 设置全屏
        window?.let { window ->
            // 设置背景透明
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // 设置布局参数 - 完全防止点击穿透
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                // 设置背景半透明
                dimAmount = 0.5f
                // 不设置任何特殊标志，让Dialog正常拦截所有触摸事件
                flags = 0
            }
            window.attributes = layoutParams
        }
        
        // 设置布局
        setContentView(R.layout.layout_ad_dialog_loading)
        
        // 设置不可取消
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
    }

    companion object {
        private var instance: ADLoadingDialog? = null

        /**
         * 显示Loading弹框
         * @param context 上下文
         */
        fun show(context: Context) {
            hide() // 先隐藏之前的实例
            
            instance = ADLoadingDialog(context)
            instance?.show()
        }

        /**
         * 隐藏Loading弹框
         */
        fun hide() {
            instance?.let { dialog ->
                if (dialog.isShowing) {
                    runCatching {
                        dialog.dismiss()
                    }
                }
            }
            instance = null
        }

        /**
         * 检查是否正在显示
         */
        fun isShowing(): Boolean {
            return instance?.isShowing ?: false
        }
    }
}

package net.corekit.monetize.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import net.corekit.monetize.R
import net.corekit.monetize.databinding.LayoutAdDialogLoadingBinding

/**
 * 全屏Loading弹框
 * 提供show和hide伴生对象函数
 * show时不允许关闭，只能通过hide关闭
 * 完全防止点击穿透
 */
class ADLoadingDialog: BaseVbDialogFragment<LayoutAdDialogLoadingBinding>() {




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = LayoutAdDialogLoadingBinding.inflate(layoutInflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
        setCancelable(false)

    }

    companion object {
        private var instance: ADLoadingDialog? = null

        /**
         * 显示Loading弹框
         * @param context 上下文
         */
        fun show(context: Context) {
            hide() // 先隐藏之前的实例
            
            instance = ADLoadingDialog()
            if(context is FragmentActivity){
                val fragmentManager = context.supportFragmentManager
                instance?.show(fragmentManager)
            }

        }

        /**
         * 隐藏Loading弹框
         */
        fun hide() {
            instance?.let { dialog ->
                if (dialog.isAdded) {
                    runCatching {
                        dialog.dismissAllowingStateLoss()
                    }
                }
            }
            instance = null
        }
    }

    override fun onDialogStyle() = R.style.AdLoadingDialog
}

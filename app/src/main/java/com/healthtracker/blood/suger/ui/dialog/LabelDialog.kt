package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.databinding.DialogLabelSelectBinding
import com.healthtracker.blood.suger.databinding.ItemLabelBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration

class LabelDialog(private val datas: List<HealthTag>?, private val addTags:List<HealthTag>?, private val onSave : ((List<HealthTag>) -> Unit)? = null) : BaseBottomSheetDialogFragment<DialogLabelSelectBinding>(){


    constructor() : this(datas = null,addTags = null, onSave = null)
    private val selectLabels = addTags?.toMutableList() ?: mutableListOf()
    companion object{
        fun show(fragmentManager: FragmentManager, healthTags:List<HealthTag>, addTags:List<HealthTag>?, onSave : (List<HealthTag>) -> Unit){
            LabelDialog(healthTags,addTags,onSave).show(fragmentManager)
        }
    }
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogLabelSelectBinding.inflate(layoutInflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
       mViewBind?.run {
           ivAdd.clickWithDuration {


           }

           ivDelete.clickWithDuration {

           }

           btnCancel.click {
               dismissAllowingStateLoss()
           }

           btnSave.click {
               onSave?.invoke(selectLabels)
               dismissAllowingStateLoss()
           }


       }

       setupLabelFlex()
    }

    private fun setupLabelFlex() {
        try {
            mViewBind?.run {
                datas?.let {
                    val labels = resources.getStringArray(R.array.blood_sugar_labels)
                    labelBox.removeAllViews()
                    for(label in it){
                        ItemLabelBinding.inflate(LayoutInflater.from(context)).apply {
                            tvLabel.text = if(label.isPreDefined == 1) labels[label.id.toInt() - 1] else label.name
                            labelBox.addView(root)
                            labelBox.flexWrap
                            root.click {
                                if(selectLabels.contains(label)){
                                    selectLabels.remove(label)
                                }else{
                                    selectLabels.add(label)
                                }
                                setupLabelFlex()
                            }

                            if(selectLabels.contains(label)){
                                tvLabel.setTextColor(ContextCompat.getColor(tvLabel.context,
                                    com.peppa.widget.picker.R.color.white))
                                labelItem.background = ContextCompat.getDrawable(labelItem.context,R.drawable.bg_label_select_selected)
                            }else{
                                tvLabel.setTextColor(ContextCompat.getColor(tvLabel.context,
                                    R.color.c5))
                                labelItem.background = ContextCompat.getDrawable(labelItem.context,R.drawable.bg_label_select_normal)
                            }
                        }


                    }
                }

            }
        }catch (e: Throwable){
            e.printStackTrace()
            dismissAllowingStateLoss()
        }
    }
}
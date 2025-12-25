package com.daily.health.manager.face.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.daily.health.manager.R

/**
 * @author wanjing 2021/4/19
 *
 * 展示反馈图片，动态排列方式
 */
class ChoosePhotoRCVAdapter(private val data: List<String>, private val listener: ChoosePhotoRCVListener) :
        RecyclerView.Adapter<ChoosePhotoRCVAdapter.RecyclerViewHolder>() {

    var picSize = 140
    var showAddPhoto = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        picSize = parent.context.resources.getDimensionPixelSize(com.healthtracker.framework.R.dimen.dp_56)
        return RecyclerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.ht_feedback_item_rcv_photo, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        when (position) {
            data.size -> {
                //拍照或选择图片 触发按钮
                holder.photoIV.setImageResource(R.drawable.ht_feedback_add_photo_dark)
                holder.deleteIV.visibility = View.GONE
            }
            else -> {
                //反馈图片
                showPhoto(holder.photoIV, data[position])
                holder.deleteIV.visibility = View.VISIBLE
            }
        }
    }

    //显示图片
    private fun showPhoto(imageView: ImageView, filePath: String) {
        try {
            Glide.with(imageView.context).load(filePath).thumbnail(0.4f).override(picSize, picSize).into(imageView)
        } catch (e: Throwable) {
            try {
                imageView.setImageBitmap(BitmapFactory.decodeFile(filePath))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = if (showAddPhoto) data.size + 1 else data.size

    inner class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var photoIV: ImageView = view.findViewById(R.id.iv_photo)
        var deleteIV: ImageView = view.findViewById(R.id.iv_delete)

        init {
            photoIV.setOnClickListener {
                //修复点击其他图片也能选择相册的问题
                if (data.isEmpty() || adapterPosition == data.size) {
                    listener.onClickAddPhoto()
                }
            }

            deleteIV.setOnClickListener {
                listener.onClickDelPhoto(adapterPosition)
            }
        }

    }

    interface ChoosePhotoRCVListener {

        fun onClickAddPhoto()

        fun onClickDelPhoto(position: Int)
    }
}
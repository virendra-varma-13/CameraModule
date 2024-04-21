package com.virendra.cameramodule.screens.allRecording

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.virendra.cameramodule.R


class RecordedListAdapter(val listData: MutableList<String>, val context: Context, val callback: (String)-> Unit) : RecyclerView.Adapter<RecordedListAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_list, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val uri = Uri.parse(listData[position])
        holder.imageView.setImageBitmap(getVideoThumbnail(uri))
        holder.textView.text = uri.lastPathSegment
        holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        holder.llItem.setOnClickListener {
            callback(listData[position])
        }
    }

    private fun getVideoThumbnail(videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val thumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        return thumbnail
    }


    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val textView: TextView = itemView.findViewById(R.id.videoName)
        val imageView: ImageView = itemView.findViewById(R.id.videoImage)
        val llItem: LinearLayout = itemView.findViewById(R.id.recordedListItem)

    }
}
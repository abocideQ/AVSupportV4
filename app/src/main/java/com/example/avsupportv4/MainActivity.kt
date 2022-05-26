package com.example.avsupportv4

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import tv.av.support.AVSupport
import tv.av.support.AVSupportAsset
import java.io.File

class MainActivity : AppCompatActivity() {

    private val mPermissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var mContentView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(mPermissions, 100)
//        }
        mContentView = findViewById(R.id.ll_content)
        mContentView.removeAllViews()
        //265 list
        val button265list = Button(baseContext)
        button265list.text = "h265 list"
        mContentView.addView(button265list)
        button265list.setOnClickListener { dO(1) }
        //265 aframe
        val button265a = Button(baseContext)
        button265a.text = "h265 首帧"
        mContentView.addView(button265a)
        button265a.setOnClickListener { dO(2) }
        //265 full frame
        val button265 = Button(baseContext)
        button265.text = "h265 fullframe"
        mContentView.addView(button265)
        button265.setOnClickListener { dO(3) }

    }

    fun dO(way: Int) {
        AVSupportAsset.assets2Sd(baseContext, "media", cacheDir.absolutePath)
        val file = File("${cacheDir.absoluteFile}/media/1s265.h265")
//        val file = File(resources.getResourcePackageName(R.drawable.))
        val h265 = when (way) {
            1 -> AVSupport().supportByList("hevc")
            2 -> AVSupport().supportByCodec(file, false, null, null, null)
            else -> AVSupport().supportByCodec(file, true, null, null, null)
        }

        val textView = TextView(baseContext)
        textView.textSize = 18f
        textView.setTextColor(Color.RED)
        textView.text = h265.exception + "\n" +
                "h265 support： ${h265.support}" + "\n" +
                "==========================" + "\n"
        mContentView.addView(textView)
    }
}
package com.example.imagelabelingdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.imagelabelingdemo.adapters.LabelAdapter
import com.example.imagelabelingdemo.models.Label
import com.example.imagelabelingdemo.utils.ImageUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private const val SELECT_PHOTO_REQUEST_CODE = 100
        private const val ASK_PERMISSION_REQUEST_CODE = 101
        private val TAG = MainActivity::class.java.name
    }

    private var mLabelList: ArrayList<Label>? = null
    private var mAdapter: LabelAdapter? = null
    private lateinit var mImageView: ImageView
    private lateinit var mLayout: CoordinatorLayout
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mDetector: FirebaseVisionImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mImageView = findViewById(R.id.imageView)
        mLayout = findViewById(R.id.main_layout)
        mRecyclerView = findViewById(R.id.recyclerView)
        mLabelList = ArrayList()
        mAdapter = LabelAdapter(mLabelList!!)
        mRecyclerView.adapter = mAdapter

        fab.setOnClickListener { view ->
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                Snackbar.make(mLayout, R.string.storage_access_required, Snackbar.LENGTH_INDEFINITE)
                    .setAction(
                        R.string.ok
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            ASK_PERMISSION_REQUEST_CODE
                        )
                    }.show()
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    ASK_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            openGallery()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            mDetector.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openGallery() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickIntent, SELECT_PHOTO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                uri?.let{
                    val file = File(ImageUtils.getRealPathFromUri(it, applicationContext))
                    val image = ImageUtils.decodeFileToSize(
                        file.absolutePath
                    )
                    mImageView.setImageBitmap(image)
                }

                mLabelList?.clear()
                mAdapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ASK_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_process -> {
                processImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun processImage() {
        if (mImageView.drawable == null) {
            Toast.makeText(applicationContext, "Please select and image first", Toast.LENGTH_LONG).show()
            return
        }

        val bitmap = ((mImageView.drawable) as BitmapDrawable).bitmap
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        mDetector = FirebaseVision.getInstance().cloudImageLabeler

        mDetector.processImage(image).addOnSuccessListener {
            val sb = StringBuilder()
            it.forEach { label ->
                val item = Label(
                    label.text,
                    label.confidence
                )
                mLabelList?.add(item)
            }

            mAdapter?.notifyDataSetChanged()
        }.addOnFailureListener {
            Log.e(TAG, "Image labelling failed ${it.localizedMessage}")
        }
    }
}

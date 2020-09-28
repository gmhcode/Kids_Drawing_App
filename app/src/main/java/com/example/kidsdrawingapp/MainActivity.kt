package com.example.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Gallery
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.ViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat())

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)


        )
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener {
            if(isReadStorageAllowed()) {
                //Run our code to get the image from the gallery
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }
        }
        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == GALLERY){
                try {
                    if(data?.data != null) {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(this@MainActivity,
                            "Error in parsing the image or its corrupted",
                            Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener{
            drawing_view.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        val medBtn = brushDialog.ib_medium_brush
        medBtn.setOnClickListener{
            drawing_view.setSizeForBrush(20f)
            brushDialog.dismiss()
        }
        val lrgBtn = brushDialog.ib_large_brush
        lrgBtn.setOnClickListener{
            drawing_view.setSizeForBrush(30f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if(view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)

            )
            mImageButtonCurrentPaint = view
        }
    }
    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())) {

                    Toast.makeText(this,
                      "Need permission to add a Background", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this,
                    "Permission granted now you can read the storage files",
                     Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this,
                    "Oops you just denied the permission",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }
    private inner class BitmapAsyncTask(val mBitmap: Bitmap): ViewModel(){

//        fun execute() = viewModelScope.launch {
//            onPreExecute()
//            val result = doInBackground()
//            onPostExecute(result)
//        }

        private lateinit var mProgressDialog: Dialog

        private fun onPreExecute() {
            showProgressDialog()
        }

//        private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
//
//            var result = ""
//
//            if(mBitmap != null){
//                try{
//                    val bytes = ByteArrayOutputStream()
//                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
//                    val f = File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")
//                    val fos = FileOutputStream(f)
//                    fos.write(bytes.toByteArray())
//                    fos.close()
//                    result = f.absolutePath
//
//                } catch (e: Exception){
//                    result = ""
//                    e.printStackTrace()
//                }
//            }
//            return@withContext result
//        }

        private fun onPostExecute(result: String?) {
            cancelDialog()
            if(!result!!.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File Saved Succesfully : $result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this@MainActivity,
                    "Something went wrong while saving file",
                    Toast.LENGTH_SHORT).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                    path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(
                    Intent.createChooser(
                        shareIntent, "Share"
                    )
                )
            }
        }

        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelDialog(){
            mProgressDialog.dismiss()
        }

    }
    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}
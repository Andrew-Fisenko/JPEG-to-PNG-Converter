package com.example.jpeg_to_png_converter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jpeg_to_png_converter.databinding.ActivityMainBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), View.OnClickListener,
    ConversionDialogFragment.OnButtonClickListener, ConverterView {

    private var pathImagePicked: String? = null
    private var pathImageConverted: String? = null
    private var isConverting: Boolean = false

    private var converterDisposable: CompositeDisposable? = null
    private lateinit var conversionDialogFragment: ConversionDialogFragment

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQUEST_CODE_GET_CONTENT = 123
        const val REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE = 124
        const val KEY_PATH_IMAGE_PICKED = "pathImagePicked"
        const val KEY_PATH_IMAGE_CONVERTED = "pathImageConverted"
        const val KEY_IS_CONVERTING = "isConverting"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imagePicked.setOnClickListener(this)
        binding.buttonConvert.setOnClickListener(this)

        requestPermissionWrite()

        savedInstanceState?.let {
            pathImagePicked = it.getString(KEY_PATH_IMAGE_PICKED)
            pathImagePicked?.let { path ->
                restoreImage(path)

                isConverting = it.getBoolean(KEY_IS_CONVERTING, false)
                if (isConverting) {
                    convertJpgToPng(
                        (binding.imagePicked.drawable as BitmapDrawable).bitmap,
                        path
                    )
                }
            }
            pathImageConverted = it.getString(KEY_PATH_IMAGE_CONVERTED)
            pathImageConverted?.let { path ->
                restoreImage(path)
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putString(KEY_PATH_IMAGE_PICKED, pathImagePicked)
            putString(KEY_PATH_IMAGE_CONVERTED, pathImageConverted)
            putBoolean(KEY_IS_CONVERTING, isConverting)
        }
    }

    override fun onDestroy() {
        converterDisposable?.dispose()
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imagePicked -> pickImage()
            R.id.buttonConvert -> {
                if (pathImagePicked == null) return

                if (checkPermissionWrite()) {
                    convertJpgToPng(
                        (binding.imagePicked.drawable as BitmapDrawable).bitmap,
                        pathImagePicked!!
                    )
                } else {
                    requestPermissionWrite()
                }
            }
        }
    }

    private fun checkPermissionWrite(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionWrite() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        binding.buttonConvert.isEnabled =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpg"))
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            REQUEST_CODE_GET_CONTENT
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_CODE_GET_CONTENT &&
            data != null
        ) {
            val imagePickedUri = data.data
            if (imagePickedUri != null) {
                setFirstImage(imagePickedUri)
            }
        }
    }

    private fun getPathFromUri(contentUri: Uri): String? {
        var res: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(projection[0])
            columnIndex.let {
                res = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return res
    }

    private fun convertJpgToPng(imagePicked: Bitmap, pathImagePicked: String) {
        isConverting = true

        conversionDialogFragment = ConversionDialogFragment(this)
        conversionDialogFragment.show(supportFragmentManager, "conversionDialogTag")
        converterDisposable = CompositeDisposable()
        converterDisposable?.add(
            ImageConverter.convertJpgToPng(imagePicked, pathImagePicked)
                .delay(3, TimeUnit.SECONDS)
                .cache()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    setSuccess(it)
                    pathImageConverted = it.first
                    isConverting = false

                    showResultImage(it.second)

                    conversionDialogFragment.dismiss()
                }, {
                    setError(it)
                    isConverting = false
                    conversionDialogFragment.dismiss()
                })
        )
    }

    override fun onPositiveClick() {
        converterDisposable?.dispose()
        conversionDialogFragment.dismiss()
    }

    override fun setFirstImage(imagePickedUri: Uri) {
        binding.imagePicked.background = null
        binding.imagePicked.setImageURI(imagePickedUri)
        pathImagePicked = getPathFromUri(imagePickedUri)
        binding.textPathImagePicked.text = pathImagePicked
    }

    override fun setError(error: Throwable) {
        Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
    }

    override fun setSuccess(it: Pair<String, Bitmap>) {
        Toast.makeText(this, "${it.first} converted to png.", Toast.LENGTH_LONG).show()
    }

    override fun showResultImage(second: Bitmap) {
        binding.imageConverted.background = null
        binding.imageConverted.setImageBitmap(second)
        binding.textPathImageConverted.text = pathImageConverted
    }

    override fun restoreImage(path: String) {
        binding.imageConverted.setImageURI(Uri.parse(path))
        binding.imageConverted.background = null
        binding.textPathImageConverted.text = path
    }
}
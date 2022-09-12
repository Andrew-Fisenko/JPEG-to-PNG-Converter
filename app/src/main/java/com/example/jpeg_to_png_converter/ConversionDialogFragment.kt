package com.example.jpeg_to_png_converter

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class ConversionDialogFragment(private val onButtonClickListener: OnButtonClickListener? = null) :
    DialogFragment() {

    interface OnButtonClickListener {

        fun onPositiveClick()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val inflater = requireActivity().layoutInflater;
            val builder = AlertDialog.Builder(it)
            builder.setView(inflater.inflate(R.layout.loader, null))
            builder.setMessage("Conversion is in progress")
                .setPositiveButton("Stop") { dialog, id ->
                    onButtonClickListener?.onPositiveClick()
                }
//            Thread(Runnable {
//               while (progressStatus < 100){
//                    progressStatus +=1
//                    Thread.sleep(100)
//                    handler.post {
//                        v.progressBar.progress = progressStatus
//                        v.textProgress.text = "$progressStatus"
//                    }
//                }
//            }).start()

            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")

    }
}
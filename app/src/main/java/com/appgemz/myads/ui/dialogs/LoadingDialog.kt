package com.appgemz.myads.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.appgemz.myads.R
import com.appgemz.myads.databinding.DialogLoadingBinding

class LoadingDialog : DialogFragment() {

    companion object {
        private const val TAG = "LoadingDialog"

        fun show(activity: AppCompatActivity) {
            if (!activity.isFinishing && !activity.supportFragmentManager.isStateSaved) {
                try {
                    val existingDialog =
                        activity.supportFragmentManager.findFragmentByTag(TAG) as? LoadingDialog
                    if (existingDialog == null || !existingDialog.isVisible) {
                        LoadingDialog().show(activity.supportFragmentManager, TAG)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun dismiss(activity: AppCompatActivity) {
            if (!activity.isFinishing && !activity.supportFragmentManager.isStateSaved) {
                try {
                    val dialog =
                        activity.supportFragmentManager.findFragmentByTag(TAG) as? LoadingDialog
                    dialog?.dismissAllowingStateLoss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private lateinit var binding: DialogLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Ad_Dialog)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }
}

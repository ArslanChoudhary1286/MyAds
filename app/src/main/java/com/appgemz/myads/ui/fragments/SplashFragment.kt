package com.appgemz.myads.ui.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appgemz.myads.databinding.FragmentSplashBinding

class SplashFragment : BaseFragment() {

    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSplashBinding.inflate(layoutInflater)

        initData()

        return binding.root
    }

    private fun initData() {

        val progressBar = binding.materialProgressBar
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        animator.duration = 10000
        animator.start()

    }

}
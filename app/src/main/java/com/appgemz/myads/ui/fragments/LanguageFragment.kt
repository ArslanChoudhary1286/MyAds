package com.appgemz.myads.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appgemz.adgemz.enums.AdPlace
import com.appgemz.myads.databinding.FragmentLanguageBinding
import com.appgemz.myads.extensions.loadNativesAd


class LanguageFragment : BaseFragment() {

    private lateinit var binding: FragmentLanguageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLanguageBinding.inflate(layoutInflater)

        initData()

        requireActivity().loadNativesAd(
            adContainer = binding.adContainer,
            AdPlace.LANGUAGE
        )

        return binding.root
    }

    private fun initData() {

        binding.btnDone.setOnClickListener {

            requireActivity().loadNativesAd(
                adContainer = binding.adContainer,
                AdPlace.LANGUAGE
            )

        }

    }

}
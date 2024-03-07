package com.mygeoimage.app.activity

import com.base.baselibrary.base.BaseActivity
import com.mygeoimage.app.R
import com.mygeoimage.app.databinding.HomePageBinding

class HomePage : BaseActivity<HomePageBinding>() {
    override fun setLayoutId(): Int {
        return R.layout.home_page
    }

    override fun initM() {
        binding.cameraBt.setOnClickListener {
            goActivity(CamPage())
        }
    }
}
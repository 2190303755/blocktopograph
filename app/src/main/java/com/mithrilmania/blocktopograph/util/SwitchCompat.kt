package com.mithrilmania.blocktopograph.util

import android.view.View
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.FragSerachAndReplaceBinding

fun FragSerachAndReplaceBinding.onCheckedChanged(checkedId: Int) {
    when (checkedId) {
        R.id.rb_search_both -> {
            this.frameSearchTwo.visibility = View.VISIBLE
            this.frameSearchOne.visibility = View.GONE
        }

        R.id.rb_search_bg, R.id.rb_search_fg, R.id.rb_search_or -> {
            this.frameSearchTwo.visibility = View.GONE
            this.frameSearchOne.visibility = View.VISIBLE
        }

        R.id.rb_place_both -> {
            this.framePlaceTwo.visibility = View.VISIBLE
            this.framePlaceOne.visibility = View.GONE
        }

        R.id.rb_place_bg, R.id.rb_place_fg -> {
            this.framePlaceTwo.visibility = View.GONE
            this.framePlaceOne.visibility = View.VISIBLE
        }
    }
}

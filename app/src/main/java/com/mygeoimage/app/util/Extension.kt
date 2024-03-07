package com.mygeoimage.app.util


import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar


 class Extension {
     companion object {
         //view visibility
         fun View.gone() = kotlin.run { this.visibility = View.GONE }
         fun View.visible() = kotlin.run { this.visibility = View.VISIBLE }
         fun View.invisible() = kotlin.run { this.visibility = View.INVISIBLE }

         infix fun View.isVisible(condition: Boolean) =
             kotlin.run { visibility = if (condition) View.VISIBLE else View.GONE }

         fun View.isVisible(): Boolean = run { visibility == View.VISIBLE }

         infix fun View.isGone(condition: Boolean) =
             kotlin.run { visibility = if (condition) View.GONE else View.VISIBLE }

         fun View.isGone(): Boolean = run { visibility == View.GONE }

         infix fun View.isInvisible(condition: Boolean) =
             kotlin.run { visibility = if (condition) View.INVISIBLE else View.VISIBLE }

         //Snackbar with view
         fun View.snackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
            Snackbar.make(this, message, duration).show()
        }

        fun View.snackbar(@StringRes message: Int, duration: Int = Snackbar.LENGTH_LONG) {
            Snackbar.make(this, message, duration).show()
        }

        fun String.showMsg(context: Context) {
            Toast.makeText(context, this + "", Toast.LENGTH_SHORT).show()
        }


        fun Float.toDp(context: Context):Int{
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
            ).toInt()
        }




        /* fun View.marginTop(context: Context,value:Float){
            this.y=this.height+value.toDp(context)
        }*/

        /*  fun View.marginTop(context: Context, value: Float) {
            val layoutParams = this.layoutParams as LinearLayout.LayoutParams
            this.y = this.height + value.toDp(context)
        }*/

    }
}


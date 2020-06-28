package me.jfenn.metronome.utils.attribouter

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import me.jfenn.attribouter.wedges.LinkWedge
import me.jfenn.metronome.Metronome

class SupportLinkWedge : LinkWedge(
        name = "Support",
        icon = "@drawable/ic_support"
) {

    override fun getListener(context: Context) = View.OnClickListener {
        (context as ContextWrapper).let { it.baseContext as AppCompatActivity }.let {
            (context.applicationContext as Metronome).buyPremium(it)
        }
    }

}
package me.jfenn.metronome.data

import androidx.annotation.RawRes
import androidx.annotation.StringRes

class TickData(
        @StringRes val nameRes: Int,
        @RawRes val soundRes: Int = -1
) {

    val isVibration = soundRes == -1

}
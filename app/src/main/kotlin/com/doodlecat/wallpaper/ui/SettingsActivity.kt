package com.doodlecat.wallpaper.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import com.doodlecat.wallpaper.R
import com.doodlecat.wallpaper.core.Prefs

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs.get(this)

        bindSegmented(R.id.row_size, listOf("Small", "Medium", "Large"),
            selected = { prefs.size.ordinal },
            choose = { prefs.putSize(Prefs.Size.of(it)) })

        bindSegmented(R.id.row_speed, listOf("Slow", "Normal", "Fast"),
            selected = { prefs.speed.ordinal },
            choose = { prefs.putSpeed(Prefs.Speed.of(it)) })

        bindSwitch(R.id.sw_rare, { prefs.rareEvents }, { prefs.putRareEvents(it) })
        bindSwitch(R.id.sw_z, { prefs.sleepingZ }, { prefs.putSleepingZ(it) })
        bindSwitch(R.id.sw_pose, { prefs.randomPose }, { prefs.putRandomPose(it) })
        bindSwitch(R.id.sw_bg, { prefs.charcoalBackground }, { prefs.putCharcoalBackground(it) })
    }

    private fun bindSegmented(
        rowId: Int,
        labels: List<String>,
        selected: () -> Int,
        choose: (Int) -> Unit
    ) {
        val row = findViewById<TextView>(rowId).parent as android.view.ViewGroup
        val options = mutableListOf<TextView>()
        for (i in row.childCount - 1 downTo 0) {
            val child = row.getChildAt(i)
            if (child is TextView && child.tag == "opt") options.add(0, child)
        }
        // Options are declared in XML in order with tag="opt".
        fun refresh() {
            val sel = selected()
            options.forEachIndexed { i, tv -> tv.isSelected = (i == sel) }
        }
        options.forEachIndexed { i, tv ->
            tv.text = labels[i]
            tv.setOnClickListener {
                choose(i)
                refresh()
            }
        }
        refresh()
    }

    private fun bindSwitch(id: Int, get: () -> Boolean, set: (Boolean) -> Unit) {
        val sw = findViewById<Switch>(id)
        sw.isChecked = get()
        sw.setOnCheckedChangeListener { _, checked -> set(checked) }
    }
}

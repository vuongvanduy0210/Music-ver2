package com.vuongvanduy.music.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.vuongvanduy.music.R
import com.vuongvanduy.music.model.ThemeMode

class ThemeModeAdapter(context: Context, resource: Int, objects: MutableList<ThemeMode>) :
    ArrayAdapter<ThemeMode>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selected, parent, false)
            view?.findViewById<TextView>(R.id.tv_selected)?.let {
                it.text = getItem(position)?.getName() ?: return@let
            }
            return view
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_theme_mode, parent, false)
        }

        view?.findViewById<TextView>(R.id.tv_them_mode)?.let {
            it.text = getItem(position)?.getName() ?: return@let
        }
        return view
    }
}
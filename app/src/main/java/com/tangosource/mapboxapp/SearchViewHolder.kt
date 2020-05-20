package com.tangosource.mapboxapp

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Roberto Ávalos on 17/05/20.
 */

class SearchViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    val street: TextView = itemView.findViewById(R.id.tvStreet)
    val remainingAddress: TextView = itemView.findViewById(R.id.tvRemainingAddress)
}
package com.tangosource.mapboxapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.api.geocoding.v5.models.CarmenFeature

/**
 * Created by Roberto Ávalos on 17/05/20.
 */

class SearchAdapter(private val listener: SearchListener) :
    RecyclerView.Adapter<SearchViewHolder>() {

    private var addresses: MutableList<CarmenFeature>? = null

    fun updateAddressList(addresses: MutableList<CarmenFeature>) {
        this.addresses = addresses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_address, parent, false)
        return SearchViewHolder(view)
    }

    override fun getItemCount(): Int = addresses?.size ?: 0

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val address = addresses!![position]
        setAddress(address.placeName(), holder)

        holder.itemView.setOnClickListener {
            listener.onSelectAddress(address)
        }
    }

    private fun setAddress(address: String?, holder: SearchViewHolder) {
        if (address != null) {
            val index = address.indexOf(",")
            val street = address.substring(0, index)
            val restOfAddress = address.substring(index + 2)
            holder.street.text = street
            holder.remainingAddress.text = restOfAddress
        }
    }
}
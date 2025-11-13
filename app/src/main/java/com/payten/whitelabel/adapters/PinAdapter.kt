package com.payten.whitelabel.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.payten.whitelabel.R
import com.payten.whitelabel.enums.PinNumber

class PinAdapter(var context : Context) : RecyclerView.Adapter<PinAdapter.PinViewHolder>(){
    private var dataList = emptyList<PinNumber>()
    private lateinit var mListener : OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(position : Int)
    }

    fun setOnItemClickListener(listener : OnItemClickListener){
        mListener = listener
    }

    internal fun setDataList(dataList: List<PinNumber>) {
        this.dataList = dataList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.pin_button, parent, false)
        return PinViewHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        when(dataList[position]) {
            PinNumber.PIN_1 -> holder.button.text = "1"
            PinNumber.PIN_2 -> holder.button.text = "2"
            PinNumber.PIN_3 -> holder.button.text = "3"
            PinNumber.PIN_4 -> holder.button.text = "4"
            PinNumber.PIN_5 -> holder.button.text = "5"
            PinNumber.PIN_6 -> holder.button.text = "6"
            PinNumber.PIN_7 -> holder.button.text = "7"
            PinNumber.PIN_8 -> holder.button.text = "8"
            PinNumber.PIN_9 -> holder.button.text = "9"
            PinNumber.PIN_10 -> {
                holder.button.visibility = View.GONE
            }
            PinNumber.PIN_11 -> holder.button.text = "0"
            PinNumber.PIN_12 -> {
                holder.button.setIconResource(R.drawable.icon_delete)
                holder.button.setIconTintResource(R.color.black)
                holder.button.text = ""
            }
        }
    }
    override fun getItemCount(): Int {
        return dataList.size
    }

    class PinViewHolder(itemView : View, listener: OnItemClickListener) : RecyclerView.ViewHolder(itemView){
        val button : MaterialButton = itemView.findViewById(R.id.pinButton)

        init {
            button.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}
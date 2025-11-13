package com.payten.whitelabel.utils

import com.payten.whitelabel.enums.PinNumber

class KeyboardUtil {

    companion object {
        private const val RESULT_BACK = "-1"
        private const val EMPTY_BUTTON = ""

        fun populateKeyboardDataList(dataList : MutableList<PinNumber>){
            dataList.add(PinNumber.PIN_1)
            dataList.add(PinNumber.PIN_2)
            dataList.add(PinNumber.PIN_3)
            dataList.add(PinNumber.PIN_4)
            dataList.add(PinNumber.PIN_5)
            dataList.add(PinNumber.PIN_6)
            dataList.add(PinNumber.PIN_7)
            dataList.add(PinNumber.PIN_8)
            dataList.add(PinNumber.PIN_9)
            dataList.add(PinNumber.PIN_10)
            dataList.add(PinNumber.PIN_11)
            dataList.add(PinNumber.PIN_12)
        }

         fun pinNumberValue(pinNumber : PinNumber) : String{
            return when(pinNumber){
                PinNumber.PIN_1 -> "1"
                PinNumber.PIN_2 -> "2"
                PinNumber.PIN_3 -> "3"
                PinNumber.PIN_4 -> "4"
                PinNumber.PIN_5 -> "5"
                PinNumber.PIN_6 -> "6"
                PinNumber.PIN_7 -> "7"
                PinNumber.PIN_8 -> "8"
                PinNumber.PIN_9 -> "9"
                PinNumber.PIN_10 -> EMPTY_BUTTON
                PinNumber.PIN_11 -> "0"
                PinNumber.PIN_12 -> RESULT_BACK
            }
        }
    }
}
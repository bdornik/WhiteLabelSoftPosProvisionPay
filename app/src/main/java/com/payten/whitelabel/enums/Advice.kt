package com.payten.whitelabel.enums


enum class Advice(private val toString: String) {
    FORCE_REACTIVATION("Force Reactivation");

    override fun toString(): String {
        return toString
    }
}
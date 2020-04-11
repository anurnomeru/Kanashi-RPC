package ink.anur

import ink.anur.inject.Republic
import ink.anur.test.WhatEverInterface

/**
 * Created by Anur IjuoKaruKas on 2020/4/10
 */
@Republic
class Provider : WhatEverInterface {

    /**
     * 实现 provider 的逻辑
     */
    override fun rpc(str1: String, long: Long): List<Any> {
        return arrayListOf("Anur", str1, "LaLaLa", long)
    }
}
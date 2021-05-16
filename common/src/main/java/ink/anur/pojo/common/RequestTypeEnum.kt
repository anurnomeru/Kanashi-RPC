package ink.anur.pojo.common

import ink.anur.exception.KanashiException
import java.util.*

/**
 * Created by Anur IjuoKaruKas on 2020/2/22
 */
enum class RequestTypeEnum(val byteSign: Int) {

    SEND_LICENSE(8000),

    SEND_LICENSE_RESPONSE(8001),

    HEAT_BEAT(9999),

    SYN(10000),

    SYN_RESPONSE(10001),

    CANVASS(10002),

    VOTING(10003),

    APPEND_ENTRIES(10004),

    PROPOSAL(10005),

    PROPOSAL_RESPONSE(10006),

    RPC_REGISTRATION(99999),

    RPC_REGISTRATION_RESPONSE(99998),

    RPC_PROVIDER_MAPPING(99997),

    RPC_REQUEST(98999),

    RPC_RESPONSE(98998),
    ;

    companion object {
        private val byteSignMap = HashMap<Int, RequestTypeEnum>()

        init {
            val unique = mutableSetOf<Int>()
            for (value in values()) {
                if (!unique.add(value.byteSign)) {
                    throw KanashiException("RequestTypeEnum 中，byteSign 不可重复。");
                }
                byteSignMap[value.byteSign] = value;
            }
        }

        fun parseByByteSign(byteSign: Int): RequestTypeEnum =
                byteSignMap[byteSign]
                        ?: throw UnsupportedOperationException("can not receive byte sign $byteSign")
    }
}
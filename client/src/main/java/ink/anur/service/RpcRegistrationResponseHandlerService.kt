package ink.anur.service

import ink.anur.connector.KanashiClientConnector
import ink.anur.core.common.AbstractRequestMapping
import ink.anur.inject.NigateBean
import ink.anur.pojo.common.RequestTypeEnum
import ink.anur.pojo.rpc.RpcRegistrationResponse
import io.netty.channel.Channel
import java.nio.ByteBuffer

/**
 * Created by Anur IjuoKaruKas on 2020/4/10
 */
@NigateBean
class RpcRegistrationResponseHandlerService : AbstractRequestMapping() {

    @NigateBean
    private lateinit var kanashiClientConnector: KanashiClientConnector

    override fun typeSupport(): RequestTypeEnum {
        return RequestTypeEnum.RPC_REGISTRATION_RESPONSE
    }

    override fun handleRequest(fromServer: String, msg: ByteBuffer, channel: Channel) {
        kanashiClientConnector.notify(RpcRegistrationResponse(msg).getSign())

    }
}
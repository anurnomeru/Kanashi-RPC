package ink.anur.service.rpc

import ink.anur.common.KanashiExecutors
import ink.anur.common.KanashiRunnable
import ink.anur.core.common.AbstractRequestMapping
import ink.anur.core.request.MsgProcessCentreService
import ink.anur.core.rpc.RpcRegistrationCenterService
import ink.anur.inject.NigateAfterBootStrap
import ink.anur.inject.NigateBean
import ink.anur.inject.NigateInject
import ink.anur.pojo.common.RequestTypeEnum
import ink.anur.pojo.rpc.RpcInetSocketAddress
import ink.anur.pojo.rpc.RpcRegistration
import ink.anur.pojo.rpc.RpcRegistrationResponse
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.nio.ByteBuffer
import java.net.InetSocketAddress
import java.util.AbstractQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * Created by Anur IjuoKaruKas on 2020/4/9
 *
 * provider 向 注册中心注册自己的服务
 */
@NigateBean
class RpcRegistrationHandlerService : AbstractRequestMapping() {

    @NigateInject
    private lateinit var msgProcessCentreService: MsgProcessCentreService

    @NigateInject
    private lateinit var rpcRegistrationCenterService: RpcRegistrationCenterService

    private val whatever = Any()

    private val sendCountingQueue = LinkedBlockingDeque<Any>()

    @NigateAfterBootStrap
    fun whileRPCMetaChange() {
        val t = Thread {
            while (true) {
                if (sendCountingQueue.poll(30, TimeUnit.SECONDS) != null) {
                    for (server in rpcRegistrationCenterService.getEnableServerMapping()) {
                        try {
                            msgProcessCentreService.sendAsync(server, rpcRegistrationCenterService.getProviderMapping())
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
        t.name = "RpcRegistrationHandlerService"
        KanashiExecutors.execute(t)
    }

    /**
     * todo 每当信息改变就从头到尾通知一次，目前暂时不做优化方案
     */
    fun notifyAllClient() {
        sendCountingQueue.offer(whatever)
    }

    override fun typeSupport(): RequestTypeEnum {
        return RequestTypeEnum.RPC_REGISTRATION
    }

    override fun handleRequest(fromServer: String, msg: ByteBuffer, channel: Channel) {
        val rpcRegistration = RpcRegistration(msg)
        val rpcRegistrationMeta = rpcRegistration.rpcRegistrationMeta

        val isa = channel.remoteAddress() as InetSocketAddress
        val name = rpcRegistrationCenterService.register(fromServer, RpcInetSocketAddress(isa.hostName, rpcRegistrationMeta.port), rpcRegistrationMeta)
        channel.pipeline().addLast(UnRegisterHandler(name, rpcRegistrationCenterService, this))

        // 进行多一次检查，避免在 addLast 后，服务就不可用了
        if (!channel.isActive) {
            rpcRegistrationCenterService.unRegister(name)
            notifyAllClient()
        }
        notifyAllClient()
        msgProcessCentreService.sendAsync(fromServer, RpcRegistrationResponse(rpcRegistrationMeta.SIGN))
    }

    class UnRegisterHandler(private val serverName: String, private val rpcRegistrationCenterService: RpcRegistrationCenterService, private val rpcRegistrationHandlerService: RpcRegistrationHandlerService) : ChannelInboundHandlerAdapter() {
        override fun channelInactive(ctx: ChannelHandlerContext?) {
            super.channelInactive(ctx)
            rpcRegistrationCenterService.unRegister(serverName)
            rpcRegistrationHandlerService.notifyAllClient()
        }
    }
}
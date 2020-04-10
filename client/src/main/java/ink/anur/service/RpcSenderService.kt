package ink.anur.service

import ink.anur.core.client.ClientOperateHandler
import ink.anur.core.request.MsgProcessCentreService
import ink.anur.exception.KanashiNoneMatchRpcProviderException
import ink.anur.inject.NigateBean
import ink.anur.inject.NigateInject
import ink.anur.pojo.rpc.RpcInetSocketAddress
import ink.anur.pojo.rpc.RpcRequest
import ink.anur.pojo.rpc.RpcRequestMeta
import ink.anur.pojo.rpc.RpcResponse
import ink.anur.rpc.RpcSender
import ink.anur.util.ClassMetaUtil
import java.lang.reflect.Method
import java.nio.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

/**
 * Created by Anur IjuoKaruKas on 2020/4/7
 *
 * 负责发送 rcp 请求
 */
@NigateBean
class RpcSenderService : RpcSender {

    @NigateInject
    private lateinit var msgProcessCentreService: MsgProcessCentreService

    @NigateInject
    private lateinit var rpcProviderMappingHolderService: RpcProviderMappingHolderService

    private val waitingMapping = ConcurrentHashMap<Long, CountDownLatch>()

    private val responseMapping = ConcurrentHashMap<Long, RpcResponse>()

    private val random = Random(100)

    private var index = 0

    // todo 添加超时机制
    override fun sendRpcRequest(method: Method, interfaceName: String, alias: String?, args: Array<out Any>?): Any? {
        val msgSign = random.nextLong()
        val cdl = CountDownLatch(1)

        return if (waitingMapping.putIfAbsent(msgSign, cdl) != null) {
            sendRpcRequest(method, interfaceName, alias, args)
        } else {
            val rpcRequest = RpcRequest(RpcRequestMeta(alias, interfaceName, ClassMetaUtil.methodSignGen(method), args, msgSign))

            val searchValidProvider = rpcProviderMappingHolderService.searchValidProvider(rpcRequest)

            if (searchValidProvider == null || searchValidProvider.isEmpty()) {
                throw KanashiNoneMatchRpcProviderException("无法找到相应的 RPC 提供者！")
            }

            responseMapping[msgSign]
        }
    }


    private val openConnections = mutableMapOf<String, ClientOperateHandler>()

    /**
     * 尝试从已经有的管道或者重新发起一个长连接
     */
    fun getOrConnectToAChannel(providers: Map<String, RpcInetSocketAddress>): Channel {
        ClientOperateHandler()
    }
}
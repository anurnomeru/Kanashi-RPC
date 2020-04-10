package ink.anur.connector

import ch.qos.logback.core.pattern.FormatInfo
import ink.anur.common.KanashiExecutors
import ink.anur.config.ClientInetSocketAddressConfiguration
import ink.anur.core.client.ClientOperateHandler
import ink.anur.core.request.MsgProcessCentreService
import ink.anur.debug.Debugger
import ink.anur.inject.Nigate
import ink.anur.inject.NigateAfterBootStrap
import ink.anur.inject.NigateBean
import ink.anur.inject.NigateInject
import ink.anur.inject.NigatePostConstruct
import ink.anur.pojo.rpc.RpcRegistration
import ink.anur.pojo.rpc.RpcRegistrationMeta
import java.lang.StringBuilder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Created by Anur IjuoKaruKas on 2020/4/8
 *
 * 客户端连接获取者
 */
@NigateBean
class KanashiClientConnector {

    @NigateInject
    private lateinit var inetSocketAddressConfiguration: ClientInetSocketAddressConfiguration

    @NigateInject
    private lateinit var msgProcessCentreService: MsgProcessCentreService

    private val debugger = Debugger(this::class.java)

    /**
     * 避免每次连接都从 0 开始
     */
    private var nowConnectCounting = Random(1).nextInt()

    @Volatile
    private var connection: ClientOperateHandler? = null

    private val random = Random(1)

    private var notifyNo = random.nextInt()

    /**
     * 发送 provider 信息到 nameServer 后进行阻塞，等待回复
     */
    private var notifyMap = mutableMapOf<Int, CountDownLatch>()

    @NigatePostConstruct
    fun connectToServer() {
        KanashiExecutors.execute(Runnable {
            val cluster = inetSocketAddressConfiguration.getCluster()
            val size = cluster.size

            while (true) {
                val nowIndex = nowConnectCounting % size
                nowConnectCounting++
                val connectLatch = CountDownLatch(1)
                val nowConnectNode = cluster[nowIndex]

                debugger.info("正在向服务器 $nowConnectNode 发起连接")
                val connect = ClientOperateHandler(nowConnectNode,
                    {
                        connectLatch.countDown()
                    },
                    {
                        connection = null
                        connectToServer()
                        false
                    })

                connect.start()
                if (connectLatch.await(5, TimeUnit.SECONDS)) {
                    connection = connect
                    debugger.info("与服务器 $nowConnectNode 建立连接，正在向节点发送节点的所有可用 RPC 信息")

                    while (notifyMap[this.notifyNo] != null) {
                        notifyNo = random.nextInt()
                    }

                    val notifyNo = notifyNo
                    val countDownLatch = CountDownLatch(1)
                    notifyMap[notifyNo] = countDownLatch

                    // 向服务器 发送本地 RPC 所有可用信息
                    msgProcessCentreService.sendAsync(nowConnectNode.serverName, RpcRegistration(
                        RpcRegistrationMeta(
                            notifyNo,
                            Nigate.getRpcBeanPath(),
                            Nigate.getRpcInterfacePath()
                        )))

                    // 如果 5 秒钟没有收到回复，重新发送可用信息
                    if (!countDownLatch.await(10, TimeUnit.SECONDS)) {
                        connect.shutDown()
                    } else {
                        debugger.info("收到服务器 $nowConnectNode 的 RPC 注册回调")
                        break// 代表成功了
                    }
                } else {
                    debugger.info("与服务器 $nowConnectNode 的连接超时")
                }
            }
        })
    }

    /**
     * 收到来自服务器的应答后解除 countdown
     */
    fun notify(sign: Int) {
        notifyMap[sign]?.countDown()
    }
}
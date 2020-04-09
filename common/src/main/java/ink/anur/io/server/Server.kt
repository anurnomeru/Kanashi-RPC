package ink.anur.io.server

import ink.anur.common.KanashiRunnable
import ink.anur.io.common.ShutDownHooker
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * Created by Anur IjuoKaruKas on 2020/2/22
 *
 * 作为 server 端的抽象父类，暴露了可定制的 channelPipelineConsumer，
 * 接入了打印错误的 ErrorHandler，注册了 shutDownHooker 可供停止此server
 */
abstract class Server( var port: Int?, private val shutDownHooker: ShutDownHooker, private val startLatch: CountDownLatch?) : KanashiRunnable() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 暴露可定制的 channelPipelineConsumer
     */
    abstract fun channelPipelineConsumer(channelPipeline: ChannelPipeline): ChannelPipeline

    /**
     * 停止这个 server
     */
    fun shutDown() {
        shutDownHooker.shutdown()
    }

    /**
     * 启动这个 server
     */
    override fun run() {
        val group = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {

                    override fun initChannel(socketChannel: SocketChannel) {
                        channelPipelineConsumer(socketChannel.pipeline())
                    }
                })
                // 保持连接
                .childOption(ChannelOption.SO_KEEPALIVE, true)


            var f: ChannelFuture?
            if (port == null) {
                while (true) {
                    val randomPort = Random(1).nextInt(60000, 65535)
                    try {
                        f = serverBootstrap.bind(randomPort)
                        port = randomPort
                        break
                    } catch (t: Throwable) {
                        // ignore
                    }
                }
            } else {
                f = serverBootstrap.bind(port!!)
            }
            f!!

            f.addListener { future ->
                if (!future.isSuccess) {
                    logger.error("监听端口 {} 失败！项目启动失败！", port)
                    exitProcess(1)
                } else {
                    startLatch?.countDown()
                    logger.info("服务器启动成功，监听端口 {}", port)
                }
            }

            shutDownHooker.shutDownRegister { group.shutdownGracefully() }

            f.channel()
                .closeFuture()
                .sync()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            try {
                group.shutdownGracefully()
                    .sync()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }
}
package github.javaguide.remoting.transport.socket;

import github.javaguide.config.CustomShutdownHook;
import github.javaguide.config.RpcServiceConfig;
import github.javaguide.factory.SingletonFactory;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.provider.impl.ZkServiceProviderImpl;
import github.javaguide.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static github.javaguide.remoting.transport.netty.server.NettyRpcServer.PORT;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 08:01:00
 * server端服务对接两个外部系统，一个是client端，一个是zookeeper注册中心
 * 所以这个类里面的方法(动作)，也都是跟着两端交互用的
 */
@Slf4j
public class SocketRpcServer {

    private final ExecutorService threadPool;
    private final ServiceProvider serviceProvider;

    public SocketRpcServer() {
        /**
         * 创建了一个线程池
         * 用来跟client端交互的时候，处理client端的请求
         */
        threadPool = ThreadPoolFactoryUtil.createCustomThreadPoolIfAbsent("socket-server-rpc-pool");
        // 这个zookeeper好粗糙，直接就是指定了这个类，里面 construction 直接给new出来了
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    public void start() {
        try (ServerSocket server = new ServerSocket()) {
            /**
             * 监听的端口就是服务端注册到zookeeper上的端口
             */
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, PORT));
            CustomShutdownHook.getCustomShutdownHook().clearAll();
            Socket socket;
            /**
             *  监听客户端连接并处理请求，server.accept是一个阻塞方法，会一直等待客户端连接
             */
            while ((socket = server.accept()) != null) {
                log.info("client connected [{}]", socket.getInetAddress());
                /**
                 * SocketRpcRequestHandlerRunnable 这个才是重点！
                 * 这里要等到client端调用到这里的时候断点进去看，服务端内部一个线程池，当然也有netty接收的办法
                 */
                threadPool.execute(new SocketRpcRequestHandlerRunnable(socket));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            log.error("occur IOException:", e);
        }
    }

}

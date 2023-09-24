import github.javaguide.HelloService;
import github.javaguide.config.RpcServiceConfig;
import github.javaguide.remoting.transport.socket.SocketRpcServer;
import github.javaguide.serviceimpl.HelloServiceImpl;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
public class SocketServerMain {

    /**
     * 先看这个，简单一点
     * (1) 先启动服务端，
     *
     * @param args
     */
    public static void main(String[] args) {
        /**
         * server端优化：
         * 这块是比较简单的示意，直接就自己把service给new出来，然后写到config里面去了
         * 如果正常的话，这里可以搞个扫描啥的
         */
        HelloService helloService = new HelloServiceImpl();
        SocketRpcServer socketRpcServer = new SocketRpcServer();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        rpcServiceConfig.setService(helloService);
        socketRpcServer.registerService(rpcServiceConfig);

        /**
         * start这里可以断点进去看，docker把zookeeper启动起来看
         */
        socketRpcServer.start();
    }
}

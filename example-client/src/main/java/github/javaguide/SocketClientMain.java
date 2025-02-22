package github.javaguide;

import github.javaguide.config.RpcServiceConfig;
import github.javaguide.proxy.RpcClientProxy;
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.remoting.transport.socket.SocketRpcClient;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
public class SocketClientMain {
    public static void main(String[] args) {
        /**
         * 每个类设计出来应该都是解决一个问题的，都一共有哪些问题？？
         */
        RpcRequestTransport rpcRequestTransport = new SocketRpcClient();
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcRequestTransport, rpcServiceConfig);

        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        String hello = helloService.hello(new Hello("111", "222"));

        System.out.println(hello);
    }
}

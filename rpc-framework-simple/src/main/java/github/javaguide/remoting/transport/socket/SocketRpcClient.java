package github.javaguide.remoting.transport.socket;

import github.javaguide.enums.ServiceDiscoveryEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.transport.RpcRequestTransport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于 Socket 传输 RpcRequest
 *
 * @author shuang.kou
 * @createTime 2020年05月10日 18:40:00
 */
@AllArgsConstructor
@Slf4j
public class SocketRpcClient implements RpcRequestTransport {
    // 服务发现组件
    private final ServiceDiscovery serviceDiscovery;

    // 构造函数，初始化时加载服务发现组件
    public SocketRpcClient() {
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
    }

    /**
     * 发送 RPC 请求
     * 这里直接就把对象写进去了，也没有编码层
     *
     * @param rpcRequest RPC请求
     * @return 服务端返回的对象
     */
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 通过服务发现组件获取服务地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        try (Socket socket = new Socket()) {
            // 连接到服务地址
            socket.connect(inetSocketAddress);
            // 创建对象输出流
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // 通过输出流向服务端发送数据
            objectOutputStream.writeObject(rpcRequest);
            // 创建对象输入流
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 从输入流中读取服务端返回的对象
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // 发生异常时，抛出 RPC 异常
            throw new RpcException("调用服务失败:", e);
        }
    }
}

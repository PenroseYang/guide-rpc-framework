package github.javaguide.remoting.transport.socket;

import github.javaguide.factory.SingletonFactory;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.handler.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 09:18:00
 */
@Slf4j
public class SocketRpcRequestHandlerRunnable implements Runnable {
    private final Socket socket;
    private final RpcRequestHandler rpcRequestHandler;


    public SocketRpcRequestHandlerRunnable(Socket socket) {
        this.socket = socket;
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    /**
     * 这是一个重写的run方法，用于处理来自客户端的请求。
     * 通过线程的方式处理，提高了处理效率。
     * 使用了try-with-resources语句，自动关闭资源，避免了资源泄露。
     * 通过ObjectInputStream和ObjectOutputStream进行对象的序列化和反序列化，实现了对象的网络传输。
     * 通过RpcRequestHandler处理请求，得到处理结果，并通过RpcResponse返回给客户端。
     * 如果在处理过程中出现异常，会进行捕获，并打印异常信息。
     */
    @Override
    public void run() {
        log.info("server handle message from client by thread: [{}]", Thread.currentThread().getName());
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
            Object result = rpcRequestHandler.handle(rpcRequest);
            objectOutputStream.writeObject(RpcResponse.success(result, rpcRequest.getRequestId()));
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            log.error("occur exception:", e);
        }
    }


}

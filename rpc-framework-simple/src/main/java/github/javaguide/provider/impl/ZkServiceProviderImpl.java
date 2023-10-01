package github.javaguide.provider.impl;

import github.javaguide.config.RpcServiceConfig;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.enums.ServiceRegistryEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.registry.ServiceRegistry;
import github.javaguide.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shuang.kou
 * @createTime 2020年05月13日 11:23:00
 */
@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc服务名（接口名+版本+组）；value: 服务对象
     * 这两个map和set，set用来标识哪个处理过了，map用作查询
     */
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;

    /**
     * 这个serviceRegistry是读取spi读出来的，写在固定的配置文件里面
     */
    private final ServiceRegistry serviceRegistry;

    // 构造函数，初始化服务映射表，已注册服务集合，以及服务注册中心
    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();

        /**
         * spi生成扩展类
         * 每一个实现类都有两部分组成，一部分是这个类对应的接口，接口名同时也是配置文件名
         * 另一部分是接口对应的策略类型，比如zk
         */
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class)
                .getExtension(ServiceRegistryEnum.ZK.getName());
    }

    // 添加服务到服务映射表中，如果服务已经注册过，则不再重复注册
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    // 从服务映射表中获取服务，如果服务不存在，则抛出异常
    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    /**
     * publishService方法是用于发布服务的。它首先获取本地主机的IP地址，
     * 然后将服务添加到服务提供者中，最后在服务注册中心注册服务，服务的地址是本地主机的IP地址和Netty服务器的端口。
     *
     * @param rpcServiceConfig rpc服务相关属性
     */
    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            // 获取本地主机IP地址
            String host = InetAddress.getLocalHost().getHostAddress();
            // 添加服务到服务提供者中
            this.addService(rpcServiceConfig);
            // 在服务注册中心注册服务，服务的地址是本地主机的IP地址和Netty服务器的端口
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }

}


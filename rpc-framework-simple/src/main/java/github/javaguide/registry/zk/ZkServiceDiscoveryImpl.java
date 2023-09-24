package github.javaguide.registry.zk;

import github.javaguide.enums.LoadBalanceEnum;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.loadbalance.LoadBalance;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.registry.zk.util.CuratorUtils;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 基于zookeeper的服务发现实现类
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    // 负载均衡器
    private final LoadBalance loadBalance;

    // 构造函数中初始化负载均衡器
    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    /**
     * 根据RpcRequest查找服务
     *
     * @param rpcRequest Rpc请求
     * @return 服务的socket地址
     */
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        // 获取Rpc服务名称
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // 获取zookeeper客户端
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        // 获取zookeeper中的服务url列表
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        // 如果服务url列表为空，抛出服务未找到异常
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // 使用负载均衡器选择一个服务地址
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("成功找到服务地址:[{}]", targetServiceUrl);
        // 解析服务地址为host和port
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        // 返回服务的socket地址
        return new InetSocketAddress(host, port);
    }
}

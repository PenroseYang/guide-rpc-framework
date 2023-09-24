package github.javaguide.registry.zk;

import github.javaguide.registry.ServiceRegistry;
import github.javaguide.registry.zk.util.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * service registration  based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 10:56:00
 */
@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {

    /**
     * registerService方法是用于在Zookeeper中注册服务的。
     * 它首先构造了一个服务路径，该路径由Zookeeper的根注册路径、服务名称和服务地址组成。
     * 然后，它获取了一个Zookeeper客户端，并在Zookeeper中创建了一个持久节点，节点的路径就是之前构造的服务路径。
     *
     * @param rpcServiceName    rpc service name
     * @param inetSocketAddress service address
     */
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}

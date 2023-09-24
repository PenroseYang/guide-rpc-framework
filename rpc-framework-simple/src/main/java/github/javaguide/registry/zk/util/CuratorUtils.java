package github.javaguide.registry.zk.util;

import github.javaguide.enums.RpcConfigEnum;
import github.javaguide.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Curator(zookeeper client) utils
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 11:38:00
 */
@Slf4j
public final class CuratorUtils {

    // 基础睡眠时间
    private static final int BASE_SLEEP_TIME = 1000;
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // Zookeeper的根节点路径
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    // 服务地址映射
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    // 已注册的路径集合
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    // Zookeeper客户端
    private static CuratorFramework zkClient;
    // 默认的Zookeeper地址
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private CuratorUtils() {
    }

    /**
     * 创建持久节点。
     * 与临时节点不同，客户端断开连接时，持久节点不会被移除
     *
     * @param path 节点路径
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            /**
             * 如果路径已存在，则不再创建
             * 内存里维护了本地缓存
             */
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("节点已存在，节点为:[{}]", path);
            } else {
                // 创建节点，如果父节点不存在，则一并创建
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("节点创建成功，节点为:[{}]", path);
            }
            // 将路径添加到已注册的路径集合中
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("创建路径[{}]的持久节点失败", path);
        }
    }

    /**
     * 获取指定节点下的所有子节点
     *
     * @param rpcServiceName rpc服务名称，例如：github.javaguide.HelloServicetest2version1
     * @return 指定节点下的所有子节点
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        // 如果服务地址映射中已存在，则直接返回
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            // 获取子节点
            result = zkClient.getChildren().forPath(servicePath);
            // 将结果放入服务地址映射中
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            // 注册监听器
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("获取路径[{}]的子节点失败", servicePath);
        }
        return result;
    }

    /**
     * 清空注册中心的数据
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        // 遍历已注册的路径集合，删除对应的节点
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("清空路径[{}]的注册中心失败", p);
            }
        });
        log.info("服务器上的所有注册服务已清空:[{}]", REGISTERED_PATH_SET.toString());
    }

    /**
     * 获取Zookeeper客户端连接
     * 如果客户端已经启动，直接返回，否则创建新的连接
     *
     * @return 返回Zookeeper客户端连接
     */
    public static CuratorFramework getZkClient() {
        /**
         * 读取配置文件，检查用户是否设置了zk地址
         * 优化：
         * 这里的zookeeper地址也是写死的，写在 properties 文件里面改不了
         */
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null &&
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;

        // 如果zkClient已经启动，直接返回
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }

        // 设置重试策略。重试3次，每次重试之间的睡眠时间会增加。
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);

        // 创建并启动Zookeeper客户端
        zkClient = CuratorFrameworkFactory.builder()
                // 要连接的服务器（可以是服务器列表）
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();

        try {
            // 等待30s直到连接到zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("连接ZK超时!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return zkClient;
    }


    /**
     * 注册监听指定节点的变化
     *
     * @param rpcServiceName rpc服务名称，例如：github.javaguide.HelloServicetest2version
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}


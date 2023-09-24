package github.javaguide.loadbalance.loadbalancer;

import github.javaguide.loadbalance.AbstractLoadBalance;
import github.javaguide.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * refer to dubbo consistent hash load balance: https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 * <p>
 * 这段代码是实现一致性哈希负载均衡的逻辑。一致性哈希算法是一种特殊的哈希算法，对于输入的同一个值，输出的结果总是相同的。
 * 这种算法在分布式系统中有广泛的应用，例如在负载均衡中，可以根据请求的 key 值（例如用户 ID）计算出一个固定的服务器地址，
 * 这样可以保证同一个用户的请求总是被路由到同一个服务器。
 *
 * @author RicardoZ
 * @createTime 2020年10月20日 18:15:20
 */
@Slf4j
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    // 使用 ConcurrentHashMap 存储每个服务的一致性哈希选择器
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 获取服务地址列表的哈希码
        int identityHashCode = System.identityHashCode(serviceAddresses);
        // 根据 rpcRequest 构建 rpc 服务名称
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // 从 selectors 中获取对应服务的一致性哈希选择器
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // 检查选择器是否存在或者服务地址列表的哈希码是否有变化，如果有变化则重新创建选择器
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        // 使用选择器选择一个服务地址
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        // 使用 TreeMap 存储虚拟节点  todo treeMap是啥？这里很多算法都不会
        private final TreeMap<Long, String> virtualInvokers;
        // 服务地址列表的哈希码
        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            // 对每个服务地址生成虚拟节点并存储到 TreeMap 中
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        // 这里把hashCode和url做了映射，写在这个map里面
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        // 使用 MD5 算法计算哈希值
        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        // 根据 digest 和 idx 计算哈希值
        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        // 根据 rpc 服务键选择一个服务地址
        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        // 根据哈希值在 TreeMap 中选择一个服务地址
        public String selectForKey(long hashCode) {
            /**
             * 一致性哈希这里得看一些算法 todo
             *
             * Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
             * 这行代码的作用是获取哈希值大于等于输入参数 hashCode 的所有虚拟节点，并返回哈希值最小的那个节点。
             * tailMap 方法的第一个参数是起始的哈希值，第二个参数 true 表示包含起始哈希值。
             * firstEntry 方法则是获取哈希值最小的节点。
             */
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }
    }
}


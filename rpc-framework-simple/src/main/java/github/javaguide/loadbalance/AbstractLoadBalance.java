package github.javaguide.loadbalance;

import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.utils.CollectionUtil;

import java.util.List;

/**
 * AbstractLoadBalance 是一个抽象的负载均衡策略类，实现了 LoadBalance 接口
 * 提供了选择服务地址的默认实现，具体的选择策略由子类实现
 *
 * @author shuang.kou
 * @createTime 2020年06月21日 07:44:00
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    /**
     * 选择服务地址
     * 如果服务地址列表为空，返回 null
     * 如果服务地址列表只有一个，直接返回该地址
     * 否则，调用 doSelect 方法由子类实现具体的选择策略
     *
     * @param serviceAddresses 服务地址列表
     * @param rpcRequest       RPC请求
     * @return 选中的服务地址
     */
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }

    /**
     * 抽象方法，由子类实现具体的服务地址选择策略
     *
     * @param serviceAddresses 服务地址列表
     * @param rpcRequest RPC请求
     * @return 选中的服务地址
     */
    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

}

package org.leo.server.panama.vpn.proxy.impl;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.log4j.Logger;
import org.leo.server.panama.vpn.configuration.ShadowSocksConfiguration;
import org.leo.server.panama.vpn.proxy.AbstractShadowSocksProxy;
import org.leo.server.panama.vpn.security.wrapper.Wrapper;
import org.leo.server.panama.vpn.security.wrapper.WrapperFactory;
import org.leo.server.panama.vpn.shadowsocks.ShadowsocksRequestResolver;
import org.leo.server.panama.vpn.util.Callback;

/**
 * @author xuyangze
 * @date 2018/11/20 8:13 PM
 */
public class AgentShadowSocksProxy extends AbstractShadowSocksProxy {
    private final static Logger log = Logger.getLogger(AgentShadowSocksProxy.class);

    protected Wrapper agentWrapper;

    public AgentShadowSocksProxy(Channel clientChannel,
                                 Callback finish,
                                 ShadowSocksConfiguration shadowSocksConfiguration,
                                 NioEventLoopGroup eventLoopGroup,
                                 ShadowsocksRequestResolver requestResolver) {
        super(clientChannel, finish, shadowSocksConfiguration, eventLoopGroup, requestResolver);
        agentWrapper = WrapperFactory.getInstance(shadowSocksConfiguration.getProxyType(), shadowSocksConfiguration.getProxyPassword(), shadowSocksConfiguration.getEncrypt());
    }

    @Override
    protected void send2Client(byte[] data) {
        if (!shadowSocksConfiguration.isProxyEqualsCurrent()) {
            // 协议不一致，则直接解密后再加密返回给客户端
            data = agentWrapper.unwrap(data);
            data = wrapper.wrap(data);
        }

        clientChannel.write(Unpooled.wrappedBuffer(data));
        clientChannel.flush();
        log.info("client <----------------  proxy " + data.length + " byte");
    }

    @Override
    public void doProxy(byte []data) {
        String target = shadowSocksConfiguration.getProxy();
        int port = shadowSocksConfiguration.getProxyPort();
        doProxy(data, target, port);
    }

    protected void doProxy(byte []data, String target, int port) {
        log.info("client ---------------->  proxy " + data.length + " byte");

        byte []decryptData = null;
        if (shadowSocksConfiguration.isProxyEqualsCurrent()) {
            // 协议一致，则直接转发，否则解密后再加密转发
            decryptData = data;
        } else {
            // 直接解密然后加密转发
            decryptData = wrapper.unwrap(data);
            decryptData = agentWrapper.wrap(decryptData);
        }

        sendRequest2Target(decryptData, target, port);
    }
}
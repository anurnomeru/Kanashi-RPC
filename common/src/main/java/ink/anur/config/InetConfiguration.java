package ink.anur.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import ink.anur.common.struct.RepublicNode;
import ink.anur.inject.config.Configuration;
import ink.anur.inject.config.ConfigurationIgnore;

/**
 * Created by Anur on 2020/9/16
 */
@Configuration(prefix = "inet")
public class InetConfiguration {
    private String localServerAddr;
    private String clientAddr;
    private long timeoutMs = 2000L;

    @ConfigurationIgnore
    private List<RepublicNode> allCache;

    @ConfigurationIgnore
    private RepublicNode local;

    @Nonnull
    public List<RepublicNode> getCluster() {
        if (allCache == null) {
            synchronized (this) {
                if (allCache == null) {
                    allCache = Stream.of(clientAddr.split(";"))
                                     .map(RepublicNode.Companion::construct)
                                     .collect(Collectors.toList());
                }
            }
        }

        return allCache;
    }

    @Nonnull
    public String getLocalServerAddr() {
        return localServerAddr;
    }

    @Nonnull
    public RepublicNode getLocalServer() {
        if (local == null) {
            synchronized (this) {
                if (local == null) {
                    local = RepublicNode.Companion.construct(localServerAddr);
                }
            }
        }

        return local;
    }

    @Nonnull
    public Long getTimeoutMs() {
        return timeoutMs;
    }
}

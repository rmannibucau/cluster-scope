package com.github.rmannibucau.hazelcast.internal;

import com.hazelcast.config.UrlXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.util.context.ContextualInstanceInfo;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import java.io.IOException;

public class ClusterScopeExtension implements Extension {
    private HazelcastInstance instance;
    private String mapName;

    void addClusterScope(final @Observes AfterBeanDiscovery afb, final BeanManager bm) {
        mapName = ConfigResolver.getPropertyValue(ClusterScopeExtension.class.getName() + ".hazelcast-ref", "cluster-scope-map");
        afb.addContext(new ClusterContext());
    }

    void addClusterScope(final @Observes BeforeShutdown bs) {
        if (instance.getCluster().getMembers().size() == 1) {
            for (final ContextualInstanceInfo<?> info : getHazelcastStorage().values()) {
                info.getCreationalContext().release();
            }
        }
        instance.getLifecycleService().shutdown();
    }

    public IMap<String, ContextualInstanceInfo<?>> getHazelcastStorage() {
        try {
            ensureInstance();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return instance.getMap(mapName);
    }

    public ILock getHazecastLock(final String name) throws IOException {
        try {
            ensureInstance();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return instance.getLock(name);
    }

    private void ensureInstance() throws IOException {
        if (instance == null) { // can already be initialized when called from ContextualStorage
            instance = Hazelcast.getHazelcastInstanceByName(ConfigResolver.getPropertyValue(ClusterScopeExtension.class.getName() + ".hazelcast-instance", "cluster-scope-instance"));
            if (instance == null) {
                final String hazelcastXml = ConfigResolver.getPropertyValue(ClusterScopeExtension.class.getName() + ".hazelcast-xml");
                instance = hazelcastXml == null ? Hazelcast.newHazelcastInstance() : Hazelcast.newHazelcastInstance(new UrlXmlConfig(hazelcastXml));
            }
        }
    }
}

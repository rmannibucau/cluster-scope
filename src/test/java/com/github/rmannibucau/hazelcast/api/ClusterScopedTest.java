package com.github.rmannibucau.hazelcast.api;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.apache.deltaspike.core.util.context.ContextualInstanceInfo;
import org.apache.openejb.Injector;
import org.junit.Test;

import javax.ejb.embeddable.EJBContainer;
import javax.inject.Inject;
import javax.naming.NamingException;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClusterScopedTest {
    @Inject
    private ClusterBean bean;

    @Test
    public void cluster() throws NamingException {
        final EJBContainer container = EJBContainer.createEJBContainer();
        Injector.inject(this);

        assertNull(bean.getName());

        final String beanId = "MANAGED#class com.github.rmannibucau.hazelcast.api.ClusterScopedTest$ClusterBean#@javax.enterprise.inject.Any(),@javax.enterprise.inject.Default(),";
        // simulate another node
        {
            final HazelcastInstance anotherInstance = Hazelcast.newHazelcastInstance();
            final IMap<String, ContextualInstanceInfo<?>> map = anotherInstance.getMap("cluster-scope-map");
            final ContextualInstanceInfo<?> contextualInstanceInfo = map.get(beanId);
            final ClusterBean instance = ClusterBean.class.cast(contextualInstanceInfo.getContextualInstance());
            instance.setName("cluster");
            map.put(beanId, contextualInstanceInfo);
            anotherInstance.getLifecycleService().shutdown();
        }

        // check injection was updated
        assertEquals("cluster", bean.getName());

        {
            final HazelcastInstance anotherInstance = Hazelcast.newHazelcastInstance();
            final IMap<String, ContextualInstanceInfo<?>> map = anotherInstance.getMap("cluster-scope-map");

            bean.setName("foo");

            final ContextualInstanceInfo<?> contextualInstanceInfo = map.get(beanId);
            final ClusterBean instance = ClusterBean.class.cast(contextualInstanceInfo.getContextualInstance());
            assertEquals("foo", instance.getName());
            anotherInstance.getLifecycleService().shutdown();
        }

        container.close();
    }

    @ClusterScoped
    public static class ClusterBean implements Serializable {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}

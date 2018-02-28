package com.smoketurner.dropwizard.consul;

import com.google.common.collect.ImmutableList;
import io.dropwizard.util.Duration;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsulFactoryTest {

    @Test
    public void testEquality() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testNotEqual() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        expected.setAdminPort(200);
        assertThat(actual).isNotEqualTo((expected));
    }

    @Test
    public void testHashCode() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
    }

    @Test
    public void testMutatedHashCode() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        expected.setAdminPort(200);
        assertThat(actual.hashCode()).isNotEqualTo(expected.hashCode());
    }

    private ConsulFactory createFullyPopulatedConsulFactory() {
        final ConsulFactory consulFactory = new ConsulFactory();
        consulFactory.setSeviceName("serviceName");
        consulFactory.setEnabled(true);
        consulFactory.setServicePort(1000);
        consulFactory.setAdminPort(2000);
        consulFactory.setServiceAddress("localhost");
        consulFactory.setTags(ImmutableList.of("tag1", "tag2"));
        consulFactory.setCheckInterval(Duration.seconds(1));
        consulFactory.setAclToken("acl-token");
        return consulFactory;
    }

}

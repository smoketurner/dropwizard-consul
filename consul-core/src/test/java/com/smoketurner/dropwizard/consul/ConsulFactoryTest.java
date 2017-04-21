package com.smoketurner.dropwizard.consul;

import com.google.common.collect.ImmutableList;
import io.dropwizard.util.Duration;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class ConsulFactoryTest {

    @Test
    public void testEquality() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        assertThat(actual, is(expected));
    }

    @Test
    public void testNotEqual() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        expected.setAdminPort(200);
        assertThat(actual, is(not(expected)));
    }

    @Test
    public void testHashCode() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        assertThat(actual.hashCode(), is(expected.hashCode()));
    }

    @Test
    public void testMutatedHashCode() {
        final ConsulFactory actual = createFullyPopulatedConsulFactory();
        final ConsulFactory expected = createFullyPopulatedConsulFactory();
        expected.setAdminPort(200);
        assertThat(actual.hashCode(), is(not(expected.hashCode())));
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
        return consulFactory;
    }

}

package com.pulseops.transaction_service.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class ProductionDemoSeedRunnerTest {
    @Test
    void missingSafetyFlagFailsClosed() {
        var runner = new ProductionDemoSeedRunner(mock(DemoDataSeeder.class),
                mock(ConfigurableApplicationContext.class), false, "demo", "seed");
        assertThrows(IllegalStateException.class, () -> runner.run());
    }

    @Test
    void nonDemoUsernameFailsClosed() {
        var runner = new ProductionDemoSeedRunner(mock(DemoDataSeeder.class),
                mock(ConfigurableApplicationContext.class), true, "operator", "seed");
        assertThrows(IllegalStateException.class, () -> runner.run());
    }

    @Test
    void resetAndReseedAreRejected() {
        var runner = new ProductionDemoSeedRunner(mock(DemoDataSeeder.class),
                mock(ConfigurableApplicationContext.class), true, "demo", "reseed");
        assertThrows(IllegalStateException.class, () -> runner.run());
    }
}

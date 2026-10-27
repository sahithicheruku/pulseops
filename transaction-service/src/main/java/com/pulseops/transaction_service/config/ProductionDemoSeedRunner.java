package com.pulseops.transaction_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo-seed-production")
public class ProductionDemoSeedRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionDemoSeedRunner.class);
    private final DemoDataSeeder seeder;
    private final ConfigurableApplicationContext context;
    private final boolean enabled;
    private final String username;
    private final String action;

    public ProductionDemoSeedRunner(DemoDataSeeder seeder, ConfigurableApplicationContext context,
            @Value("${app.demo.seed.enabled:false}") boolean enabled,
            @Value("${app.demo.username:}") String username,
            @Value("${app.demo.seed.action:seed}") String action) {
        this.seeder = seeder;
        this.context = context;
        this.enabled = enabled;
        this.username = username;
        this.action = action;
    }

    @Override
    public void run(String... args) {
        if (!enabled) throw new IllegalStateException("Production demo seed requires DEMO_SEED_ENABLED=true");
        if (!"demo".equals(username)) throw new IllegalStateException("Production demo seed requires DEMO_USERNAME=demo");
        if (!"seed".equalsIgnoreCase(action)) throw new IllegalStateException("Production demo seed only supports DEMO_SEED_ACTION=seed");
        var report = seeder.seedInsertOnly();
        log.info("event=production_demo_seed_completed totalBefore={} nonDemoBefore={} demoBefore={} inserted={} totalAfter={} nonDemoAfter={} demoAfter={}",
                report.totalBefore(), report.nonDemoBefore(), report.demoBefore(), report.inserted(),
                report.totalAfter(), report.nonDemoAfter(), report.demoAfter());
        SpringApplication.exit(context);
    }
}

package de.leipzig.htwk.gitrdf.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

import javax.sql.DataSource;

@Configuration
public class LockConfig {

    private static final int THIRTY_MINUTES = 1000 * 60 * 30;

    @Bean
    public DefaultLockRepository defaultLockRepository(DataSource dataSource) {

        DefaultLockRepository defaultLockRepository = new DefaultLockRepository(dataSource);
        defaultLockRepository.setTimeToLive(THIRTY_MINUTES);

        return defaultLockRepository;
    }

    @Bean
    public JdbcLockRegistry jdbcLockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }

}

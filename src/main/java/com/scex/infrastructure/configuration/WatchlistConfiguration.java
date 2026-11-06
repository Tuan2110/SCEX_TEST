package com.scex.infrastructure.configuration;

import com.scex.application.usecase.MergeWatchlist;
import com.scex.domain.service.WatchlistMergeEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WatchlistConfiguration {
    @Bean
    WatchlistMergeEngine watchlistMergeEngine() {
        return new WatchlistMergeEngine();
    }

    @Bean
    MergeWatchlist mergeWatchlist(WatchlistMergeEngine engine) {
        return new MergeWatchlist(engine);
    }
}

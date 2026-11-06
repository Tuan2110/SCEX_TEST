package com.scex;

import com.scex.application.usecase.MergeWatchlist;
import com.scex.domain.entity.Item;
import com.scex.domain.entity.Operation;
import com.scex.domain.entity.Watchlist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ScexTestApplicationTests {
    @Autowired
    private MergeWatchlist mergeWatchlist;

    @Test
    void configuredMergeUseCaseExecutes() {
        var result = mergeWatchlist.execute(Watchlist.of(), List.of(),
                List.of(Operation.add("VNM")));
        assertEquals(List.of(new Item("VNM", "")), result.watchlist().items());
    }
}

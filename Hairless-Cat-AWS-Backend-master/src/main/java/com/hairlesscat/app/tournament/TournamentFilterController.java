package com.hairlesscat.app.tournament;

import com.hairlesscat.app.util.ResponseWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TournamentFilterController {
    @GetMapping("tournament_filters")
    public Map<String, List<String>> getAllTournamentFilters() {
        List<String> res = TournamentFilter.names();
        return ResponseWrapper.wrapResponse("tournament_filters", res);
    }
}

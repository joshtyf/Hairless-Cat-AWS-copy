package com.hairlesscat.app.tournament;

import com.hairlesscat.app.util.ResponseWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TournamentParameterController {
    @GetMapping("tournament_styles")
    public Map<String, List<String>> getAllTournamentStyles() {
        return ResponseWrapper.wrapResponse("tournament_styles", TournamentStyle.names());
    }
}

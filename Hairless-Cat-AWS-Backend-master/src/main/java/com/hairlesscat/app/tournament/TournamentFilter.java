package com.hairlesscat.app.tournament;

import java.util.List;
import java.util.stream.Stream;

public enum TournamentFilter {
    UNSCHEDULED,
    SCHEDULED;

    public static List<String> names() {
        return Stream.of(TournamentFilter.values()).map(TournamentFilter::name).toList();
    }
}

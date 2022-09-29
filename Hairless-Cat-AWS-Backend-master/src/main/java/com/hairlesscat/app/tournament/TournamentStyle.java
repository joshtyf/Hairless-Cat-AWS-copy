package com.hairlesscat.app.tournament;

import java.util.List;
import java.util.stream.Stream;

public enum TournamentStyle {
    SINGLE_ROUND_ROBIN,
    DOUBLE_ROUND_ROBIN,
	BRACKET,
    SINGLE_KO;

    public static List<String> names() {
        return Stream.of(TournamentStyle.values()).map(TournamentStyle::name).toList();
    }
}

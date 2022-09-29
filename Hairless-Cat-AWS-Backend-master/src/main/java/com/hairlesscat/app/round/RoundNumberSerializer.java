package com.hairlesscat.app.round;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;

import java.io.IOException;
import java.util.Map;

public class RoundNumberSerializer extends JsonSerializer<Map<TournamentTimeslot, Integer>> {
	@Override
	public void serialize(Map<TournamentTimeslot, Integer> tournamentTimeslotRoundMap, JsonGenerator gen,
						  SerializerProvider provider) throws IOException {
		gen.writeStartArray();

		for (TournamentTimeslot tts : tournamentTimeslotRoundMap.keySet()) {
			gen.writeStartObject();
			gen.writeNumberField("tournamentTimeslot_id", tts.getTimeslotId());
			gen.writeNumberField("round_number", tournamentTimeslotRoundMap.get(tts));
			gen.writeEndObject();
		}

		gen.writeEndArray();
	}
}

/*
package com.hairlesscat.app.result;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hairlesscat.app.team.Team;

import java.io.IOException;
import java.util.Map;

public class ConfirmValSerializer extends JsonSerializer<Map<Team, Boolean>> {
	@Override
	public void serialize(Map<Team, Boolean> teamStatusMap, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeStartArray();

		for (Team team : teamStatusMap.keySet()) {
			gen.writeStartObject();
			gen.writeNumberField("team_id", team.getTeamId());
			gen.writeStringField("team_confirmation", teamStatusMap.get(team).toString());
			gen.writeEndObject();
		}

		gen.writeEndArray();
	}


}

 */

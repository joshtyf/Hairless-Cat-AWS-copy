package com.hairlesscat.app.result;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hairlesscat.app.team.Team;

import java.io.IOException;
import java.util.Map;

public class ConfirmValSerializer extends JsonSerializer<Map<Team, Boolean>> {
	@Override
	public void serialize(Map<Team, Boolean> teamConfirmationMap, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeStartArray();

		for (Team team : teamConfirmationMap.keySet()) {
			gen.writeStartObject();
			gen.writeNumberField("team_id", team.getTeamId());
			gen.writeBooleanField("team_confirmation", teamConfirmationMap.get(team));
			gen.writeEndObject();
		}

		gen.writeEndArray();
	}


}

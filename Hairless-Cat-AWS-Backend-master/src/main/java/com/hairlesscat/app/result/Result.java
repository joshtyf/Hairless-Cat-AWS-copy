package com.hairlesscat.app.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hairlesscat.app.match.Match;
import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.view.Views;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result {
	@Id
	@SequenceGenerator(
		name = "result_sequence_generator",
		sequenceName = "result_sequence_generator",
		allocationSize = 1
	)
	@GeneratedValue(
		generator = "result_sequence_generator",
		strategy = GenerationType.SEQUENCE
	)
	@Column(name = "result_id", updatable = false)
	@JsonProperty("result_id")
	@JsonView(Views.Public.class)
	private Long resultId;

	@ManyToMany(cascade = CascadeType.PERSIST)
	@JoinTable(
		name= "result_teams",
		joinColumns = @JoinColumn(name = "team_id"),
		inverseJoinColumns = @JoinColumn(name = "result_id"))
	@JsonView(Views.ResultSummary.class)
	@JsonProperty("team_results")
	private List<Team> rankedTeam;

	@OneToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(
		name = "result_match_id",
		referencedColumnName = "match_id"
	)
	@JsonProperty("result_match")
	@JsonView(Views.ResultFull.class)
	private Match match;

	@ElementCollection
	@CollectionTable(
		name = "result_team_confirmation_map",
		joinColumns = @JoinColumn(name = "result_id", referencedColumnName = "result_id"))
	@MapKeyJoinColumn(name = "team_id", referencedColumnName = "team_id")
	@Column(name = "team_confirmation")
	@JsonView(Views.ResultFull.class)
	@JsonSerialize(using = ConfirmValSerializer.class)
	@JsonDeserialize(using = ConfirmValDeserializer.class)
	@JsonProperty("team_confirmations")
	private Map<Team, Boolean> teamConfirmationMap = new HashMap<>();

	public Result(List<Team> rankedTeamArr, Match match){
		this.rankedTeam = rankedTeamArr;
		this.match = match;
	}

	public Result(Long resultId, List<Team> rankedTeamArr, Match match){
		this.rankedTeam = rankedTeamArr;
		this.match = match;
	}
}

import {setLoading} from "../App";
import TournamentList from "./TournamentList";

import React, {useContext, useState} from "react";
import {CompleteTeam, CompleteTournament} from "../global";

function teamsReady(t: CompleteTournament): true | [CompleteTeam[], CompleteTeam[]] {
    const members: CompleteTeam[] = [];
    const availability: CompleteTeam[] = [];
    for (let team of t.teams as CompleteTeam[]) {
        if (team.team_members.length < t.tournament_parameters.min_number_of_players_per_team) {
            members.push(team);
        } else if (!team.has_indicated_availability) {
            availability.push(team);
        }
    }
    return (members.length === 0 && availability.length === 0) || [members, availability];
}

function canBeScheduled(t: CompleteTournament) {
    const teamCount = t.teams.length === t.tournament_parameters.required_number_of_teams;
    return teamCount && teamsReady(t) === true;
}

function showTeamCount(t: CompleteTournament) {
    const ma = teamsReady(t);
    let warning = undefined;
    if (ma !== true) {
        const [members, availability] = ma;
        warning = [
            <>{members.length !== 0 &&
                <h2 key="1">Team{members.length > 1 ? "s" : ""} {members.map(team => `${team.team_name} (needs ${t.tournament_parameters.required_number_of_teams - team.team_members.length})`).join(", ")} {members.length > 1 ? "do" : "does"} not
                    have enough members</h2>
            }</>,
            <>{availability.length !== 0 &&
                <h2 key="0">Team{availability.length > 1 ? "s" : ""} {availability.map(t => t.team_name).join(", ")} {availability.length > 1 ? "have" : "has"} not
                    submitted availability</h2>
            }</>
        ];
    }
    return [
        <h2 key="1">{t.teams.length} teams, {t.tournament_parameters.required_number_of_teams} required</h2>,
        <>{warning}</>,
        t.tournament_schedule.schedule_status !== "SCHEDULE_NOT_GENERATED" &&
        <h2 key="2">{t.tournament_schedule.schedule_status_error_message}</h2>
    ];
}

export default function ScheduleTournament() {
    const load = useContext(setLoading);
    const [refresh, setRefresh] = useState(false);

    function scheduleTournament(t: CompleteTournament) {
        load("i");

        async function e() {
            await fetch(
                `https://backend.hairless.brycemw.ca/tournaments/actions/gen_match_schedule/${t.tournament_id}`,
                {
                    method: "POST"
                }
            );
        }

        e().finally(() => {
            load("d");
            setRefresh(!refresh);
        });
    }

    return <div className="GenerateSchedule">
        <h1>Generate Schedule</h1>
        <TournamentList
            getArgs="filter=UNSCHEDULED"
            btnName="Schedule"
            btnAction={scheduleTournament}
            btnPred={canBeScheduled}
            detail={showTeamCount}
            additional="full_teams"
            refresh={refresh}
        />
    </div>;
}

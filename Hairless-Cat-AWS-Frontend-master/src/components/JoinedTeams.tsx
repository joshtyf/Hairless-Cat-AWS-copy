import React, {useContext, useEffect, useState} from 'react';

import "../css/JoinedTeams.scss";
import {CompleteTeam, CompleteTeamTournament, Team, User} from "../global";
import {setLoading, user} from "../App";

interface TeamMap {
    [index: string]: CompleteTeamTournament
}

async function getFullTeam(t: Team) {
    let [team, tournament] = await Promise.all([
        fetch(`https://backend.hairless.brycemw.ca/teams/${t.team_id}`).then(t => t.json()),
        fetch(`https://backend.hairless.brycemw.ca/tournaments/${t.tournament!.tournament_id}`).then(t => t.json())
    ]);
    team.tournament = tournament;
    team.matches = await Promise.all(team.matches.map((m: { match_id: any; }) => fetch(`https://backend.hairless.brycemw.ca/matches/${m.match_id}`).then(m => m.json())));
    return team as CompleteTeamTournament;
}

function renderMember(m: { user: User }) {
    return <span key={m.user.user_id}>{m.user.f_name}</span>;
}

export default function JoinedTeams() {
    const load = useContext(setLoading);
    const lUser = useContext(user);

    const [teams, setTeams] = useState([]);
    const [teamMap, setTeamMap] = useState<TeamMap>({});
    const [refresh, setRefresh] = useState(false);

    useEffect(() => {
        load("i");

        async function e() {
            const res = await fetch(`https://backend.hairless.brycemw.ca/teams?user_id=${lUser.user_id}`)
            let teams = (await res.json()).teams;
            teams = await Promise.all(teams.map(getFullTeam));
            let teamMap: TeamMap = {};
            for (let team of teams) {
                teamMap[team.team_id] = team;
            }
            setTeams(teams);
            setTeamMap(teamMap);
        }

        e().finally(() => load("d"));
    }, [lUser.user_id, load, refresh]);

    function canLeave(t: CompleteTeam) {
        return !t.team_members.find((m) => m.user.user_id === lUser.user_id)?.is_leader && !t.has_indicated_availability;
    }

    function leaveTeam(t: CompleteTeam) {
        // DELETE /teams/{team_id}/users/{user_id}
        load("i");

        async function e() {
            await fetch(
                `https://backend.hairless.brycemw.ca/teams/${t.team_id}/users/${lUser.user_id}`,
                {
                    method: "DELETE"
                }
            );
            setRefresh(!refresh);
        }

        e().finally(() => load("d"))
    }

    function renderTeam(t: CompleteTeamTournament) {
        return <div key={t.team_id} data-team-id={t.team_id}>
            <h1>{t.team_name}</h1>
            <h2>{t.team_members.length} members, {t.tournament.tournament_parameters.min_number_of_players_per_team} required, {t.tournament.tournament_parameters.max_number_of_players_per_team} max</h2>
            {t.team_members.length !== 0 &&
                <p><span>Members:</span>{t.team_members.map(renderMember)}</p>
            }
            <span>
                {canLeave(t) &&
                    <button
                        onClick={e => leaveTeam(teamMap[(e.target as Element).parentElement!.parentElement!.dataset["teamId"]!] as CompleteTeamTournament)}>Leave</button>
                }
            </span>
        </div>;
    }

    return <div className="JoinedTeams">
        <h1>Joined teams</h1>
        <div className="TournamentList">
            {teams.map(renderTeam)}
        </div>
    </div>;
}

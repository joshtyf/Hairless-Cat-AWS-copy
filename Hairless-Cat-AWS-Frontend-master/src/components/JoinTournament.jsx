import {setLoading, setTopLevel, user} from "../App";
import {useCheckSize} from "./BryceUtils";
import TournamentList from "./TournamentList";

import React, {useContext, useEffect, useRef, useState} from "react";
import {useHistory} from "react-router-dom";
import FullCalendar from "@fullcalendar/react";
import timeGridPlugin from "@fullcalendar/timegrid";

import "../css/JoinTournament.scss";

// Tournaments List
function canCreate(t) {
    return t.teams.length < t.tournament_parameters.required_number_of_teams;
}

function alreadyInTeam(t, user, teamMap) {
    for (let sTeam of t.teams) {
        const team = teamMap[sTeam.team_id];
        if (team && team.team_members) {
            for (let member of team.team_members) {
                if (member.user.user_id === user.user_id) {
                    return true;
                }
            }
        }
    }
    return false;
}

export default function JoinTournament() {
    // --- Declarations and hooks ---

    // Global
    const load = useContext(setLoading);
    const lUser = useContext(user);
    const lSetTopLevel = useContext(setTopLevel);
    const history = useHistory();

    // Create Team

    // Join Team
    const calendar = useRef(null);
    const [timeslots, setTimeslots] = useState([]);
    const selectedTimeslots = useRef({});

    // Teams List
    const [view, smallView, checkSize] = useCheckSize();
    const [refreshTeams, setRefreshTeams] = useState(false);
    const [teams, setTeams] = useState([]);
    const [teamMap, setTeamMap] = useState({});
    const [team, setTeam] = useState(null);

    // Tournaments List
    const [tournament, setTournament] = useState(null);

    // --- Functions and effects ---

    // Create Team
    function createTeam() {
        const name = document.querySelector("#teamName").value;
        load("i");

        async function e() {
            await fetch(
                `https://backend.hairless.brycemw.ca/tournaments/${tournament.tournament_id}/teams`,
                {
                    method: "POST",
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({user_id: lUser.user_id, team_name: name})
                }
            );
        }

        e().finally(() => {
            load("d");
            lSetTopLevel(null);
            setRefreshTeams(!refreshTeams)
        });
    }

    function showCreateTeam() {
        lSetTopLevel(<div className="JoinTournamentTopLevel">
            <h1>Create team</h1>
            <form className="dual-row" style={{"--rows": 1}} onSubmit={e => {
                e.preventDefault();
                createTeam()
            }}>
                <span>Name</span>
                <input type="text"
                       placeholder="Bryce's team"
                       name="teamName"
                       id="teamName"
                       minLength={1}
                       pattern=".*[^\s].*"
                       required
                />
                <input type="submit" value="Create" className="submit"/>
                <div/>
            </form>
        </div>);
    }

    // Join Team
    useEffect(() => {
        if (tournament !== null) {
            load("i");

            async function getTimeslots() {
                const res = await fetch(`https://backend.hairless.brycemw.ca/tournaments/${tournament.tournament_id}/timeslots`)
                if (res.ok) {
                    const timeslots = (await res.json()).timeslots;
                    setTimeslots(timeslots.map(t => ({
                        id: t.tournament_timeslot_id,
                        start: t.timeslot_start_time,
                        end: t.timeslot_end_time
                    })));
                }
            }

            getTimeslots().finally(() => load("d"));
        }
    }, [load, tournament]);

    useEffect(() => {
        const startTime = tournament !== null ? new Date(tournament.tournament_schedule.tournament_start_time) : new Date();
        calendar.current.getApi().gotoDate(startTime);
    }, [tournament])

    function joinTeam() {
        load("i");

        async function e() {
            if (team.team_members.findIndex(e => e.user.user_id === lUser.user_id) === -1) {
                await fetch(
                    `https://backend.hairless.brycemw.ca/teams/${team.team_id}/users`,
                    {
                        method: "POST",
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({user_ids: [lUser.user_id]})
                    }
                );
            }
            await fetch(
                `https://backend.hairless.brycemw.ca/teams/${team.team_id}/member_availabilities/${lUser.user_id}`,
                {
                    method: "POST",
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({timeslot_ids: Object.keys(selectedTimeslots.current)})
                }
            );
            history.push("/");
        }

        e().finally(() => load("d"));
    }

    function clearTimeslots() {
        for (let timeslot_id in selectedTimeslots.current) {
            selectedTimeslots.current[timeslot_id]?.classList.remove("selected");
        }
        selectedTimeslots.current = {};
    }

    function selectTime(arg) {
        const event_id = arg.event.id;
        const element = arg.el;
        if (event_id in selectedTimeslots.current) {
            element.classList.remove("selected");
            delete selectedTimeslots.current[event_id];
        } else {
            element.classList.add("selected");
            selectedTimeslots.current[event_id] = element;
        }
    }

    function listTeam() {
        return <div>
            <div className="BackButton">
                <button onClick={() => setTeam(null)}>
                    <div/>
                    List
                </button>
                <button onClick={joinTeam}>Submit</button>
            </div>
            <div className="Calendar">
                <FullCalendar
                    ref={calendar}
                    plugins={[timeGridPlugin]}
                    initialView="timeGridWeek"
                    allDaySlot={false}
                    dayHeaderFormat={view}
                    windowResize={() => checkSize(smallView)}
                    events={timeslots}
                    eventClick={arg => selectTime(arg)}
                />
            </div>
        </div>;
    }

    // Teams List
    useEffect(() => {
        if (tournament !== null) {
            load("i");

            async function getTeams() {
                const res = await fetch(`https://backend.hairless.brycemw.ca/teams/?tournament_id=${tournament.tournament_id}`);
                if (res.ok) {
                    let teams = (await res.json()).teams;
                    let teamMap = {};
                    for (let team of teams) {
                        teamMap[team.team_id] = team;
                    }
                    const us = teams.find(t => t.team_members.findIndex(m => m.user.user_id === lUser.user_id) !== -1);
                    if (us) {
                        teams = [us];
                    } else {
                        teams = teams.filter(t => t.team_members.length < tournament.tournament_parameters.max_number_of_players_per_team);
                    }
                    setTeams(teams);
                    setTeamMap(teamMap);
                }
            }

            getTeams().finally(() => load("d"));
        }
    }, [load, lUser, tournament, refreshTeams]);

    function renderMember(m) {
        return <span key={m.user.user_id}>{m.user.f_name}</span>;
    }

    function renderTeam(t) {
        return <div key={t.team_id} data-team-id={t.team_id}>
            <h1>{t.team_name}</h1>
            {t.team_members.length !== 0 &&
                <p><span>Members:</span>{t.team_members.map(renderMember)}</p>
            }
            <span><button onClick={e => {
                clearTimeslots();
                setTeam(teamMap[e.target.parentElement.parentElement.dataset.teamId])
            }}>Join</button></span>
        </div>;
    }

    function listTeams() {
        return <div className="JoinTeam">
            <div className="BackButton">
                <button onClick={() => setTournament(null)}>
                    <div/>
                    List
                </button>
                <button
                    hidden={tournament === null || !canCreate(tournament) || alreadyInTeam(tournament, lUser, teamMap)}
                    onClick={showCreateTeam}>Create team
                </button>
            </div>
            <div className="TournamentList">
                {teams.map(renderTeam)}
            </div>
            {(tournament === null || !canCreate(tournament)) && teams.length === 0 &&
                <h1>All teams are full and the maximum number of teams have been created</h1>
            }
        </div>;
    }

    // Tournaments List
    function setTournamentWithDetails(t) {
        load("i");

        async function e() {
            const res = await fetch(`https://backend.hairless.brycemw.ca/tournaments/${t.tournament_id}`);
            setTournament(await res.json());
        }

        e().finally(() => load("d"))
    }

    return <div className="JoinTournament">
        <h1>Join tournament</h1>
        <div hidden={tournament !== null}>
            <TournamentList
                getArgs="filter=UNSCHEDULED"
                btnName="Join"
                btnAction={setTournamentWithDetails}
            />
        </div>
        <div hidden={tournament === null || team !== null}>
            {listTeams()}
        </div>
        <div hidden={team === null}>
            {listTeam()}
        </div>
    </div>;
}
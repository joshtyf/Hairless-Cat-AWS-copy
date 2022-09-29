import {setLoading, setTopLevel, user} from "../App";

import React, {useCallback, useContext, useEffect, useRef, useState} from "react";
import {formatDate} from "@fullcalendar/react";
import {DndProvider, useDrag, useDrop} from "react-dnd";
import {HTML5Backend} from "react-dnd-html5-backend";
import update from 'immutability-helper';

import "../css/ManageTeams.scss";

function renderMember(m) {
    return <span key={m.user.user_id}>{m.user.f_name}</span>;
}

async function getFullTeam(t) {
    let [team, tournament] = await Promise.all([
        fetch(`https://backend.hairless.brycemw.ca/teams/${t.team_id}`).then(t => t.json()),
        fetch(`https://backend.hairless.brycemw.ca/tournaments/${t.tournament.tournament_id}`).then(t => t.json())
    ]);
    team.tournament = tournament;
    team.matches = await Promise.all(team.matches.map(m => fetch(`https://backend.hairless.brycemw.ca/matches/${m.match_id}`).then(m => m.json())));
    return team;
}

function canSubmitAvailabilities(t) {
    return !t.has_indicated_availability && t.team_members.length >= t.tournament.tournament_parameters.min_number_of_players_per_team;
}

function getNextMatch(t) {
    const matches = t.matches;
    for (let match of matches) {
        if (match.team_statuses.findIndex(s => s.team_id === t.team_id && s.team_status === "PENDING") !== -1) {
            return match;
        }
    }
    return null;
}

function getUpcomingMatch(t) {
    const matches = t.matches;
    for (let match of matches) {
        if (match.match_status === "UPCOMING" || match.match_status === "COMPLETED") {
            return match;
        }
    }
    return null;
}

export default function ManageTeams() {
    const load = useContext(setLoading);
    const lUser = useContext(user);
    const lSetTopLevel = useContext(setTopLevel);

    const [teams, setTeams] = useState([]);
    const [refreshTeams, setRefreshTeams] = useState(false);
    const [teamMap, setTeamMap] = useState({});
    const cards = useRef([]);

    useEffect(() => {
        load("i");

        async function e() {
            const res = await fetch(`https://backend.hairless.brycemw.ca/teams/leader/${lUser.user_id}`)
            let teams = (await res.json()).teams;
            teams = await Promise.all(teams.map(getFullTeam));
            let teamMap = {};
            for (let team of teams) {
                teamMap[team.team_id] = team;
            }
            setTeams(teams);
            setTeamMap(teamMap);
        }

        e().finally(() => load("d"));
    }, [lUser.user_id, load, refreshTeams]);

    function submitAvailabilities(t) {
        load("i");

        async function e() {
            const res = await fetch(
                `https://backend.hairless.brycemw.ca/teams/actions/post_team_availabilities/${t.team_id}`,
                {
                    method: "POST",
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({user_id: lUser.user_id})
                }
            );
            if (!res.ok) {
                lSetTopLevel(<div style={{margin: "0 1.5em 0 0"}}>
                    {await res.text()}
                </div>);
            }
            setRefreshTeams(!refreshTeams);
        }

        e().finally(() => load("d"));
    }

    function acceptMatch(m, t) {
        load("i");

        async function e() {
            await fetch(
                `https://backend.hairless.brycemw.ca/matches/actions/${m.match_id}/accept_match/${t.team_id}`,
                {method: "POST"}
            );
        }

        e().finally(() => {
            lSetTopLevel(null);
            setRefreshTeams(!refreshTeams);
            load("d")
        })
    }

    function showAcceptMatch(t) {
        const m = getNextMatch(t);
        console.log(m);
        const test = m.teams_in_match.map(t => ({id: t.team_id, text: t.team_name}));
        console.log(test);
        cards.current = m.teams_in_match.map(t => ({id: t.team_id, text: t.team_name}));
        console.log(cards.current);
        lSetTopLevel(<div className="ManageTeamsTopLevel">
            <h1>Match</h1>
            <h2>{m.teams_in_match.map(t => t.team_name).join(" - ")}</h2>
            <span>Start: {formatDate(new Date(m.match_start_time), {
                month: 'long',
                year: 'numeric',
                day: 'numeric',
                hour: 'numeric',
                minute: 'numeric'
            })}, End: {formatDate(new Date(m.match_end_time), {
                hour: 'numeric',
                minute: 'numeric'
            })}</span>
            <button onClick={() => acceptMatch(m, t)}>Accept</button>
        </div>);
    }

    function completeMatch(m, t) {
        load("i");

        async function e() {
            await fetch(
                `https://backend.hairless.brycemw.ca/matches/${m.match_id}/complete`,
                {
                    method: "POST"
                }
            );
            await fetch(
                `https://backend.hairless.brycemw.ca/results/${m.match_id}`,
                {
                    method: "POST",
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({team_results: cards.current.map(c => c.id)})
                }
            );
            await fetch(
                `https://backend.hairless.brycemw.ca/teams/${t.team_id}/confirm_result/${m.match_id}`,
                {
                    method: "POST"
                }
            );
            setRefreshTeams(true);
        }

        e().finally(() => {
            load("d");
            lSetTopLevel(null)
        });
    }

    function showCompleteMatch(t) {
        const m = getUpcomingMatch(t);
        cards.current = m.teams_in_match.map(t => ({id: t.team_id, text: t.team_name}));
        lSetTopLevel(<div className="ManageTeamsTopLevel">
            <h1>Match</h1>
            <h2>{m.teams_in_match.map(t => t.team_name).join(" - ")}</h2>
            <span>Start: {formatDate(new Date(m.match_start_time), {
                month: 'long',
                year: 'numeric',
                day: 'numeric',
                hour: 'numeric',
                minute: 'numeric'
            })}, End: {formatDate(new Date(m.match_end_time), {
                hour: 'numeric',
                minute: 'numeric'
            })}</span>
            <h2>Drag to rank teams</h2>
            <DndProvider backend={HTML5Backend}>
                <TeamDnd cards={cards}/>
            </DndProvider>
            <button onClick={() => completeMatch(m, t)}>Accept</button>
        </div>);
    }

    function deleteTeam(t) {
        load("i");

        async function e() {
            await fetch(
                `https://backend.hairless.brycemw.ca/tournaments/${t.tournament.tournament_id}/teams/${t.team_id}`,
                {
                    method: "DELETE"
                }
            );
            setRefreshTeams(!refreshTeams);
        }

        e().finally(() => load("d"))
    }

    function renderTeam(t) {
        return <div key={t.team_id} data-team-id={t.team_id}>
            <h1>{t.team_name}</h1>
            <h2>{t.team_members.length} members, {t.tournament.tournament_parameters.min_number_of_players_per_team} required, {t.tournament.tournament_parameters.max_number_of_players_per_team} max</h2>
            {t.team_members.length !== 0 &&
                <p><span>Members:</span>{t.team_members.map(renderMember)}</p>
            }
            <span>
                {canSubmitAvailabilities(t) &&
                    <button
                        onClick={e => submitAvailabilities(teamMap[e.target.parentElement.parentElement.dataset.teamId])}>Submit
                        schedule</button>
                }
            </span>
            <span>
                {t.tournament.tournament_schedule.schedule_status === "SCHEDULE_NOT_GENERATED" &&
                    <button
                        onClick={e => deleteTeam(teamMap[e.target.parentElement.parentElement.dataset.teamId])}>Delete
                        team</button>
                }
            </span>
            <span>
                {getNextMatch(t) !== null &&
                    <button
                        onClick={e => showAcceptMatch(teamMap[e.target.parentElement.parentElement.dataset.teamId])}>Accept
                        match</button>
                }
            </span>
            <span>
                {getUpcomingMatch(t) !== null &&
                    <button
                        onClick={e => showCompleteMatch(teamMap[e.target.parentElement.parentElement.dataset.teamId])}>
                        Complete match
                    </button>
                }
            </span>
        </div>;
    }

    return <div className="ManageTeams">
        <h1 key={0}>Manage teams</h1>,
        <div key={1} className="TournamentList">
            {teams.map(renderTeam)}
        </div>
    </div>;
}

function TeamDnd({cards: iCards}) {
    const [cards, setCards] = useState(iCards.current);
    const moveCard = useCallback((dragIndex, hoverIndex) => {
        setCards(prevCards => {
                const res = update(prevCards, {
                    $splice: [
                        [dragIndex, 1],
                        [hoverIndex, 0, prevCards[dragIndex]],
                    ],
                });
                iCards.current = res;
                return res;
            }
        );
    }, [iCards]);
    const renderCard = useCallback((card, index) => {
        return <TeamCard
            key={card.id}
            index={index}
            id={card.id}
            text={card.text}
            moveCard={moveCard}
        />;
    }, [moveCard]);
    return <>
        <div>{cards.map((card, i) => renderCard(card, i))}</div>
    </>;
}

function TeamCard({id, text, index, moveCard}) {
    const ref = useRef(null);
    const [{handlerId}, drop] = useDrop({
        accept: "team",
        collect(monitor) {
            return {handlerId: monitor.getHandlerId()}
        },
        hover(item, monitor) {
            if (!ref.current) {
                return;
            }
            const dragIndex = item.index;
            const hoverIndex = index;
            if (dragIndex === hoverIndex) {
                return;
            }
            const hoverBoundingRect = ref.current?.getBoundingClientRect();
            const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;
            const clientOffset = monitor.getClientOffset();
            const hoverClientY = clientOffset.y - hoverBoundingRect.top;
            if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
                return;
            }
            if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
                return;
            }
            moveCard(dragIndex, hoverIndex);
            item.index = hoverIndex;
        }
    });
    const [{isDragging}, drag] = useDrag({
        type: "team",
        item: () => {
            return {id, index};
        },
        collect: monitor => ({isDragging: monitor.isDragging()})
    });
    const opacity = isDragging ? 0 : 1;
    const cursor = isDragging ? "grabbing" : "grab";
    drag(drop(ref));
    return <div ref={ref} style={{opacity, cursor}} data-handler-id={handlerId}>{text}</div>;
}

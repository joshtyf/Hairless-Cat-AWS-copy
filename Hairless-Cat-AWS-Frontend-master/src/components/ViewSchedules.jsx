import {setLoading} from "../App";
import {useCheckSize} from "./BryceUtils";
import TournamentList from "./TournamentList";

import React, {useContext, useEffect, useRef, useState} from "react";
import FullCalendar from "@fullcalendar/react";
import timeGridPlugin from "@fullcalendar/timegrid";

import "../css/ViewSchedules.scss";

function mapEvents(matches) {
    return matches.map(m => ({
        id: m.match_id,
        title: m.teams_in_match.map(t => t.team_name).join(" - "),
        start: new Date(m.match_start_time),
        end: new Date(m.match_end_time)
    }));
}

export default function ViewSchedules() {
    const load = useContext(setLoading);

    const calendar = useRef();
    const [view, smallView, checkSize] = useCheckSize();

    const [tournament, setTournament] = useState(null);
    const [matches, setMatches] = useState([]);

    useEffect(() => {
        if (tournament !== null) {
            load("i");

            async function e() {
                const res = await fetch(`https://backend.hairless.brycemw.ca/matches/?tournament_id=${tournament.tournament_id}`);
                setMatches((await res.json()).matches);
            }

            const startTime = new Date(tournament.tournament_schedule.tournament_start_time);
            calendar.current.getApi().gotoDate(startTime);

            e().finally(() => load("d"));
        }
    }, [tournament, load]);

    return <div className="ViewSchedules">
        <div hidden={tournament !== null}>
            <h1>Scheduled tournaments</h1>
            <TournamentList
                getArgs="filter=SCHEDULED"
                btnName="View schedule"
                btnAction={setTournament}
            />
        </div>
        {tournament !== null &&
            <div>
                <h1>Tournament schedule</h1>
                <div className="BackButton">
                    <button onClick={() => setTournament(null)}>
                        <div/>
                        List
                    </button>
                </div>
                <div className="Calendar">
                    <FullCalendar
                        ref={calendar}
                        plugins={[timeGridPlugin]}
                        initialView="timeGridWeek"
                        allDaySlot={false}
                        dayHeaderFormat={view}
                        windowResize={() => checkSize(smallView)}
                        events={mapEvents(matches)}
                    />
                </div>
            </div>
        }
    </div>;
}
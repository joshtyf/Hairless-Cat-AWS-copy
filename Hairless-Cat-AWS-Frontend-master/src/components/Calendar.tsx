import {setLoading, setTopLevel} from "../App";

import React, {useContext, useEffect, useState} from "react";
import FullCalendar, {EventClickArg, EventContentArg, formatDate} from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";

import '../css/Calendar.scss';
import {Tournament} from "../global";

interface TopLevelObjs {
    [index: string]: JSX.Element;
}

function mapEvents(events: Tournament[]) {
    return events.map(t => ({
        id: t.tournament_id.toString(),
        title: t.name,
        start: new Date(t.tournament_schedule.tournament_start_time),
        end: new Date(t.tournament_schedule.tournament_end_time),
        extendedProps: t
    }))
}

export default function Calendar() {
    const load = useContext(setLoading);
    const lSetTopLevel = useContext(setTopLevel);
    const [events, setEvents] = useState<Tournament[]>([]);

    let topLevelObjs: TopLevelObjs = {};

    useEffect(() => {
        load("i");

        async function e() {
            const res = await fetch("https://backend.hairless.brycemw.ca/tournaments");
            if (res.ok) {
                setEvents((await res.json()).tournaments);
            }
        }

        e().finally(() => load("d"));
    }, [load]);

    function addContent(arg: EventContentArg) {
        const tournament = arg.event.extendedProps;
        topLevelObjs[`tournament-event-${arg.event.extendedProps.tournament_id}`] = <div className="CalendarTopLevel">
            <h1>{tournament.name}</h1>
            <p>{tournament.description}</p>
            <div className="dual-row" style={{"--rows": 2}}>
                <span>Start</span>
                <span>{arg.event.start &&
                    formatDate(arg.event.start, {
                        month: 'long',
                        year: 'numeric',
                        day: 'numeric'
                    })
                }</span>
                <span>End</span>
                <span>{arg.event.end &&
                    formatDate(arg.event.end, {
                        month: 'long',
                        year: 'numeric',
                        day: 'numeric'
                    })
                }</span>
                <div/>
            </div>
        </div>;

        return <div>
            {arg.event.title}
        </div>;
    }

    function showContent(arg: EventClickArg) {
        const topLevel = topLevelObjs[`tournament-event-${arg.event.extendedProps.tournament_id}`];
        lSetTopLevel(topLevel);
    }

    return <div className="Calendar">
        <FullCalendar
            plugins={[dayGridPlugin]}
            initialView="dayGridMonth"
            events={mapEvents(events)}
            eventContent={arg => addContent(arg)}
            eventClick={arg => showContent(arg)}
        />
    </div>;


}
declare module 'csstype' {
    interface Properties {
        '--rows'?: number;
    }
}

export interface Tournament {
    name: string,
    description: string,
    status: "NOT_STARTED", // TODO(bryce)
    teams?: Team[],
    matches?: any[], // TODO(bryce)
    rounds?: Array<{ round_id: number }>,
    tournament_id: number,
    tournament_parameters: {
        tournament_parameter_id: number,
        required_number_of_teams?: number,
        min_number_of_players_per_team?: number,
        max_number_of_players_per_team?: number,
        tournament_type: "TEAM",
        tournament_style: string
    },
    tournament_schedule: {
        schedule_id: number,
        tournament_start_time: string,
        tournament_end_time: string,
        schedule_status: "SCHEDULE_NOT_GENERATED" | "OVER", // TODO(bryce)
        schedule_status_error_message?: string | null
    },
    admin_user?: null // TODO(bryce)
}

export interface CompleteTournament {
    name: string,
    description: string,
    status: "NOT_STARTED", // TODO(bryce)
    teams: Team[],
    matches: any[], // TODO(bryce)
    rounds: Array<{ round_id: number }>,
    tournament_id: number,
    tournament_parameters: {
        tournament_parameter_id: number,
        required_number_of_teams: number,
        min_number_of_players_per_team: number,
        max_number_of_players_per_team: number,
        tournament_type: "TEAM",
        tournament_style: string
    },
    tournament_schedule: {
        schedule_id: number,
        tournament_start_time: string,
        tournament_end_time: string,
        schedule_status: "SCHEDULE_NOT_GENERATED" | "OVER", // TODO(bryce)
        schedule_status_error_message: string | null
    },
    admin_user: null // TODO(bryce)
}

interface Team {
    tournament?: {
        tournament_id: number
    },
    results?: [],
    matches?: [],
    team_id: number,
    team_name: string,
    team_members?: Array<{
        user: {
            email: string,
            user_id: string,
            is_admin: boolean,
            f_name: string,
            l_name: string | null
        },
        is_leader: boolean
    }>,
    has_indicated_availability?: true
}

export interface CompleteTeam {
    tournament: {
        tournament_id: number
    },
    results: [],
    matches: [],
    team_id: number,
    team_name: string,
    team_members: Array<{
        user: {
            email: string,
            user_id: string,
            is_admin: boolean,
            f_name: string,
            l_name: string | null
        },
        is_leader: boolean
    }>,
    has_indicated_availability: true
}

export interface CompleteTeamTournament {
    tournament: CompleteTournament,
    results: [],
    matches: [],
    team_id: number,
    team_name: string,
    team_members: Array<{
        user: {
            email: string,
            user_id: string,
            is_admin: boolean,
            f_name: string,
            l_name: string | null
        },
        is_leader: boolean
    }>,
    has_indicated_availability: true
}

export interface User {
    user_id: string,
    is_admin: boolean,
    department?: string | null,
    company?: string | null,
    f_name: string,
    l_name: string | null,
    email: string,
    p_number?: string | null
}

import AppHead from "./components/AppHead";
import Calendar from "./components/Calendar";
import CreateTournament from "./components/CreateTournament";
import DeleteTournament from "./components/DeleteTournament";
import Footer from "./components/Footer";
import JoinedTeams from "./components/JoinedTeams";
import JoinTournament from "./components/JoinTournament";
import Loading from "./components/Loading";
import ManageTeams from "./components/ManageTeams";
import ScheduleTournament from "./components/ScheduleTournament";
import ViewSchedules from "./components/ViewSchedules";

import React, {createContext, useContext, useReducer, useState} from "react";
import {BrowserRouter, Route, Switch} from "react-router-dom";
import {GoogleLoginResponse, GoogleLoginResponseOffline, useGoogleLogin} from "react-google-login";

import "./css/App.scss";
import {User} from "./global";

export const clientId = '305885881555-fa3k18ajmgrlj0toth807kc80hgu0pa6.apps.googleusercontent.com';
export const loading = createContext(0);
export const setLoading = createContext((_: any) => {
});
export const user = createContext<User>({
    user_id: "none",
    is_admin: true,
    department: null,
    company: null,
    f_name: "None",
    l_name: null,
    email: `none@none.none`,
    p_number: null
});
export const setUser = createContext((_: any) => {
});
export const pfp = createContext<string | null>(null);
export const setTopLevel = createContext((_: any) => {
});

async function updateUser(user: User) {
    const res = await fetch(
        `https://backend.hairless.brycemw.ca/users/${user.user_id}`,
        {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        }
    );
    if (res.ok) {
        await fetch(
            `https://backend.hairless.brycemw.ca/users/${user.user_id}`,
            {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(user)
            }
        );
    } else {
        await fetch(
            `https://backend.hairless.brycemw.ca/users`,
            {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(user)
            }
        );
    }
}

function CheckLoggedIn({children}: { children: JSX.Element | JSX.Element[] }) {
    const load = useContext(setLoading);
    const [lUser, lSetUser] = useState<User | null>(null);
    const [lPfp, setPfp] = useState<null | string>(null);
    const [topLevel, lSetTopLevel] = useState(null);
    const googleLogin = useGoogleLogin({
        onSuccess: OnGoogleSuccess,
        onFailure: () => {
            load("d");
        },
        clientId,
        isSignedIn: true,
        accessType: "offline"
    });

    function LocalLogIn() {
        load("i");

        async function e() {
            const id = (document.querySelector(".LogIn > div > div > input:nth-of-type(1)") as HTMLInputElement).value;
            const user = {
                user_id: id,
                is_admin: true,
                department: null,
                company: null,
                f_name: id,
                l_name: null,
                email: `${id}@${id}.${id}`,
                p_number: null
            };
            await updateUser(user);
            lSetUser(user);
        }

        e().finally(() => load("d"));
    }

    function GoogleLogIn() {
        load("i");
        googleLogin.signIn();
    }

    function OnGoogleSuccess(pRes: GoogleLoginResponse | GoogleLoginResponseOffline) {
        if (pRes.code !== undefined) {
            return;
        }
        const res = pRes as GoogleLoginResponse;

        let refreshTime = (res.tokenObj.expires_in || 3600 - 5 * 60) * 1000;

        async function refreshToken() {
            try {
                const newAuthRes = await res.reloadAuthResponse();
                refreshTime = (newAuthRes.expires_in || 3600 - 5 * 60) * 1000;
                setTimeout(refreshToken, refreshTime);
            } catch (ignored) {
                window.location.reload();
            }
        }

        setTimeout(refreshToken, refreshTime);

        setPfp(res.profileObj.imageUrl);

        async function e(gUser: { googleId: string, imageUrl: string, email: string, name: string, givenName: string, familyName: string }) {
            let admin = false;
            let nUser = {
                user_id: "google-" + gUser.googleId,
                is_admin: admin,
                department: null,
                company: null,
                f_name: gUser.givenName,
                l_name: gUser.familyName || null,
                email: gUser.email || "Unknown@example.com",
                p_number: null
            }
            const res = await fetch(
                `https://backend.hairless.brycemw.ca/users/${"google-" + gUser.googleId}`,
                {
                    method: 'GET',
                    headers: {'Content-Type': 'application/json'}
                }
            );
            if (res.ok) {
                admin = (await res.json()).is_admin || false;

                nUser.is_admin = admin;
                lSetUser(nUser);
                await fetch(
                    `https://backend.hairless.brycemw.ca/users/${"google-" + gUser.googleId}`,
                    {
                        method: 'PUT',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(nUser)
                    }
                );
            } else {
                // User does not exist yet
                lSetUser(nUser);
                await fetch(
                    `https://backend.hairless.brycemw.ca/users`,
                    {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(nUser)
                    }
                );
            }
        }

        e(res.profileObj).finally(() => load("d"));
    }

    if (lUser === null) {
        return <div className="LogIn">
            <div>
                <button onClick={() => {
                    GoogleLogIn()
                }}>
                    <img src="https://upload.wikimedia.org/wikipedia/commons/5/53/Google_%22G%22_Logo.svg" alt="G"/>
                    <div>Log in with Google</div>
                </button>
                <div>
                    <input type="text" placeholder="Test user ID"/>
                    <button onClick={() => {
                        LocalLogIn()
                    }}>
                        Log in with Test User
                    </button>
                </div>
            </div>
        </div>;
    }

    return <user.Provider value={lUser}>
        <pfp.Provider value={lPfp}>
            {
                topLevel !== null &&
                <div className="TopLevel">
                    <div>
                        {topLevel}
                        <div onClick={() => lSetTopLevel(null)}/>
                    </div>
                </div>
            }
            <div style={{overflowY: "scroll"}}>
                <setUser.Provider value={lSetUser}>
                    <AppHead/>
                </setUser.Provider>
                <setTopLevel.Provider value={lSetTopLevel}>
                    {children}
                </setTopLevel.Provider>
                <Footer/>
            </div>
        </pfp.Provider>
    </user.Provider>;
}

export default function App() {
    const [isLoading, load] = useReducer<(s: number, i: "i" | "d") => number>(
        (s: number, action: "i" | "d") => {
            if (action === "i") {
                return s + 1;
            }
            if (action === "d") {
                return s - 1 >= 0 ? s - 1 : 0;
            }
            return 0; // Unreachable, just for TS to be happy
        },
        0
    );

    return <BrowserRouter>
        <loading.Provider value={isLoading}>
            <Loading/>
        </loading.Provider>
        <setLoading.Provider value={load}>
            <CheckLoggedIn>
                <div className="contents">
                    <Switch>
                        <Route exact path="/index.html" component={Calendar}/>
                        <Route exact path="/" component={Calendar}/>

                        <Route exact path="/createTournament" component={CreateTournament}/>
                        <Route exact path="/deleteTournament" component={DeleteTournament}/>
                        <Route exact path="/scheduleTournament" component={ScheduleTournament}/>
                        <Route exact path="/viewSchedules" component={ViewSchedules}/>

                        <Route exact path="/joinTournament" component={JoinTournament}/>
                        <Route exact path="/manageTeams" component={ManageTeams}/>
                        <Route exact path="/joinedTeams" component={JoinedTeams}/>
                    </Switch>
                </div>
            </CheckLoggedIn>
        </setLoading.Provider>
    </BrowserRouter>;
}

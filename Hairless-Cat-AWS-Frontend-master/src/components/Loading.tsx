import {loading} from "../App";

import React, {useContext} from "react";

import '../css/Loading.scss';

export default function Loading() {
    const isLoading = useContext(loading);
    return isLoading ? <div className="Loading">
        <div>
            <span>Loading</span><span>.</span><span>.</span><span>.</span>
        </div>
    </div> : null;
}
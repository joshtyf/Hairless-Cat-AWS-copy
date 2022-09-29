import {useCallback, useEffect, useMemo, useState} from "react";

export function useCheckSize() {
    const [smallView, setSmallView] = useState(false);
    const view = useMemo(() => {
        if (smallView) {
            return {weekday: 'short'};
        } else {
            return {weekday: 'short', month: 'numeric', day: 'numeric', omitCommas: true};
        }
    }, [smallView]);
    const checkSize = useCallback(smallView => {
        const small = window.innerWidth <= 700;
        if (!smallView && small) {
            setSmallView(true);
        } else if (smallView && !small) {
            setSmallView(false);
        }
    }, []);
    useEffect(() => {
        checkSize(false);
    }, [checkSize]);

    return [view, smallView, checkSize];
}
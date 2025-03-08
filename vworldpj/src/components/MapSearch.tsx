"use client"

import { useEffect, useRef } from "react";

const MapSearch = () => {
    const vMapContainer = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const initializeMap = () => {
            if (!window.vw || !window.vw.Map) {
                console.error("VWorld script not loaded!");
                return;
            }

            const options = {
                mapId: vMapContainer.current?.id,
                initPosition: new vw.CameraPosition(
                    new vw.CoordZ(127.425, 38.196, 1548700),
                    new vw.Direction(0, -90, 0)
                ),
                logo: true,
                navigation: true,
            };

            const map = new vw.Map();
            map.setOption(options);
            map.start();
        };

        // Load VWorld Script
        const script = document.createElement("script");
        script.src = "/vworld-map-init.js";
        script.async = true;
        script.onload = initializeMap;
        document.body.appendChild(script);

        return () => {
            script.remove();
        };
    }, []);

    return (
        <div
            id="vmap"
            ref={vMapContainer}
            className="w-full h-screen"
        />
    );
};

export default MapSearch;

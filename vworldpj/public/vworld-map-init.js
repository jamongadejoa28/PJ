// public/vworld-map-init.js
if (typeof window !== "undefined") {
    const script = document.createElement("script");
    script.src = "https://map.vworld.kr/js/webglMapInit.js.do?version=3.0&apiKey=6E9CE663-6911-306B-9982-F19C3EA3224C";
    script.async = true;
    document.body.appendChild(script);
}

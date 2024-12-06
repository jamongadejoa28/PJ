// src/app/components/Copyright.tsx
"use client";

const Copyright = () => {
  return (
    <div className="p-4">
      <h4 className="text-lg font-semibold mb-2">Icon Copyrights</h4>
      <div className="images grid grid-cols-2 gap-2 mb-4">
        <img src="/images/generate.png" alt="Generate Icon" />
        <img src="/images/passenger.png" alt="Passenger Icon" />
        <img src="/images/truck.png" alt="Truck Icon" />
        <img src="/images/bicycle.png" alt="Bicycle Icon" />
      </div>
      <p className="mb-4">
        Icons by{" "}
        <a href="http://google.com/design/" className="underline">
          Google Design
        </a>
        , licensed under{" "}
        <a
          href="http://creativecommons.org/licenses/by-sa/3.0/"
          className="underline"
        >
          CC-BY-SA
        </a>
      </p>
      <div className="images grid grid-cols-2 gap-2 mb-4">
        <img src="/images/bus.png" alt="Bus Icon" />
        <img src="/images/pedestrian.png" alt="Pedestrian Icon" />
        <img src="/images/ship.png" alt="Ship Icon" />
        <img src="/images/rail.png" alt="Rail Icon" />
      </div>
      <p className="mb-4">
        Icons by{" "}
        <a href="http://aiga.org/" className="underline">
          AIGA
        </a>
        , Open Domain
      </p>
      <div className="images grid grid-cols-2 gap-2 mb-4">
        <img src="/images/tram.png" alt="Tram Icon" />
        <img src="/images/rail_urban.png" alt="Urban Rail Icon" />
      </div>
      <p className="mb-4">
        Icons by{" "}
        <a href="http://www.danilodemarco.com/" className="underline">
          Pittogrammi
        </a>
      </p>
      <div className="images grid grid-cols-1 gap-2 mb-4">
        <img src="/images/motorcycle.png" alt="Motorcycle Icon" />
      </div>
      <p className="mb-4">
        Icon by{" "}
        <a href="http://icons8.com/" className="underline">
          Icons8
        </a>
      </p>
      <div className="images grid grid-cols-1 gap-2 mb-4">
        <img src="/images/road.png" alt="Road Icon" />
      </div>
      <p className="mb-4">
        Icon by{" "}
        <a href="https://www.flaticon.com/" className="underline">
          Flaticon
        </a>
      </p>
    </div>
  );
};

export default Copyright;

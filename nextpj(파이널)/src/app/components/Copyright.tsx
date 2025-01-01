// src/app/components/Copyright.tsx
"use client";

export default function Copyright() {
  return (
    <div className="p-4">
      <h4 className="text-lg font-semibold mb-2">Icon Copyrights</h4>
      
      <div className="images-container">
        <img src="/images/generate.png" alt="Generate Icon" className="copyright-image" />
        <img src="/images/passenger.png" alt="Passenger Icon" className="copyright-image" />
        <img src="/images/truck.png" alt="Truck Icon" className="copyright-image" />
        <img src="/images/bicycle.png" alt="Bicycle Icon" className="copyright-image" />
      </div>
      <p className="text-sm mb-4">
        Icons by{" "}
        <a href="http://google.com/design/" className="copyright-link">
          Google Design
        </a>
        , licensed under{" "}
        <a href="http://creativecommons.org/licenses/by-sa/3.0/" className="copyright-link">
          CC-BY-SA
        </a>
      </p>

      <div className="images-container">
        <img src="/images/bus.png" alt="Bus Icon" className="copyright-image" />
        <img src="/images/pedestrian.png" alt="Pedestrian Icon" className="copyright-image" />
        <img src="/images/ship.png" alt="Ship Icon" className="copyright-image" />
        <img src="/images/rail.png" alt="Rail Icon" className="copyright-image" />
      </div>
      <p className="text-sm mb-4">
        Icons by{" "}
        <a href="http://aiga.org/" className="copyright-link">
          AIGA
        </a>
        , Open Domain
      </p>

      <div className="images-container">
        <img src="/images/tram.png" alt="Tram Icon" className="copyright-image" />
        <img src="/images/rail_urban.png" alt="Urban Rail Icon" className="copyright-image" />
      </div>
      <p className="text-sm mb-4">
        Icons by{" "}
        <a href="http://www.danilodemarco.com/" className="copyright-link">
          Pittogrammi
        </a>
      </p>

      <div className="images-container">
        <img src="/images/motorcycle.png" alt="Motorcycle Icon" className="copyright-image" />
      </div>
      <p className="text-sm mb-4">
        Icon by{" "}
        <a href="http://icons8.com/" className="copyright-link">
          Icons8
        </a>
      </p>

      <div className="images-container">
        <img src="/images/road.png" alt="Road Icon" className="copyright-image" />
      </div>
      <p className="text-sm mb-4">
        Icon by{" "}
        <a href="https://www.flaticon.com/" className="copyright-link">
          Flaticon
        </a>
      </p>
    </div>
  );
}
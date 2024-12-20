// src/types/simulation.ts

export enum RoadCategory {
  Highway = "Highway",
  Railway = "Railway",
  Pedestrians = "Pedestrians",
  Aeroway = "Aeroway",
  Waterway = "Waterway",
  Aerialway = "Aerialway",
  Route = "Route"
}

export enum VehicleType {
  Passenger = "passenger",
  Truck = "truck",
  Bus = "bus",
  Motorcycle = "motorcycle",
  Bicycle = "bicycle",
  Pedestrian = "pedestrian",
  Tram = "tram",
  RailUrban = "rail_urban",
  Rail = "rail",
  Ship = "ship"
}

export interface VehicleSettings {
  count: number;
  fringeFactor: number;
  enabled: boolean;
}

export interface SimulationMessage {
  type: 'vehicle_positions' | 'simulation_complete' | 'error';
  data?: Array<{
    id: string;
    position: {
      x: number;  // UTM X 좌표
      y: number;  // UTM Y 좌표
    };
    type: string;
  }>;
  progress?: number;
  message?: string;
}

export interface ScenarioOptions {
  polygons: boolean;
  publicTransport: boolean;
  carOnlyNetwork: boolean;
  decal: boolean;
  leftHand: boolean;
}

export interface ScenarioRequest {
  coordinates: {
    lat: number;
    lng: number;
  };
  radius: number;
  duration: number;
  vehicles: Record<VehicleType, VehicleSettings>;
  roadTypes: Record<RoadCategory, string[]>;
  options: ScenarioOptions;
}

export type VehicleMarkersProps = {
  simulationActive: boolean;
  duration: number;
  onError?: () => void;
  onComplete?: () => void;
};

export type Vehicle = {
  id: string;
  position: {
    lat: number;
    lng: number;
  };
  type: string;
};
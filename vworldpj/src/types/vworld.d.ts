 // src/types/vworld.d.ts
declare global {
    namespace vw {
      interface MapOptions {
        mapId: string;
        initPosition: CameraPosition;
        logo?: boolean;
        navigation?: boolean;
        renderMode?: string;
      }
  
      class Map {
        constructor();
        setOption(options: MapOptions): void;
        start(): void;
        moveTo(position: CameraPosition, duration: number): void;
        destroy(): void;
      }
  
      class CameraPosition {
        constructor(coordZ: CoordZ, direction: Direction);
      }
  
      class CoordZ {
        constructor(x: number, y: number, z: number);
      }
  
      class Direction {
        constructor(x: number, y: number, z: number);
      }
    }
  }

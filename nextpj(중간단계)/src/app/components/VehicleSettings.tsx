// src/app/components/VehicleSettings.tsx
"use client";

interface VehicleData {
  fringeFactor: number;
  count: number;
  enabled: boolean;
}

type VehicleType =
  | "passenger"
  | "truck"
  | "bus"
  | "motorcycle"
  | "bicycle"
  | "pedestrian"
  | "tram"
  | "rail_urban"
  | "rail"
  | "ship";

interface VehicleSettingProps {
  display: string;
  internal: VehicleType;
  defaultFringeFactor: number;
  defaultCount: number;
  defaultEnabled: boolean;
  onChange?: (data: VehicleData) => void;
}

export default function VehicleSettings({
  display,
  internal,
  defaultFringeFactor,
  defaultCount,
  defaultEnabled,
  onChange,
}: VehicleSettingProps) {
  const handleChange = (field: keyof VehicleData, value: number | boolean) => {
    if (onChange) {
      onChange({
        fringeFactor: defaultFringeFactor,
        count: defaultCount,
        enabled: defaultEnabled,
        [field]: value,
      });
    }
  };

  return (
    <div className="container">
      <div className="flex items-center justify-between">
        <h4 className="flex items-center gap-2">
          <img
            src={`/images/${internal}.png`}
            alt={display}
            className="h-5 w-5"
          />
          {display}
        </h4>
        <input
          type="checkbox"
          checked={defaultEnabled}
          onChange={(e) => handleChange("enabled", e.target.checked)}
          className="checkbox"
        />
      </div>
      {defaultEnabled && (
        <div className="options mt-2 space-y-2">
          <label className="option-label">
            Through Traffic Factor
            <input
              type="number"
              value={defaultFringeFactor}
              onChange={(e) =>
                handleChange("fringeFactor", parseFloat(e.target.value) || 0)
              }
              min={0.5}
              max={100}
              step={0.1}
              className="number-input"
            />
          </label>
          <label className="option-label">
            Count
            <input
              type="number"
              value={defaultCount}
              onChange={(e) =>
                handleChange("count", parseFloat(e.target.value) || 0)
              }
              min={0.2}
              max={100}
              step={0.1}
              className="number-input"
            />
          </label>
        </div>
      )}
    </div>
  );
}

export type { VehicleType, VehicleData };
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
  // 값이 변경될 때 숫자로 확실하게 변환
  const handleChange = (field: keyof VehicleData, value: number | boolean) => {
    if (onChange) {
      let processedValue = value;

      // 숫자 타입인 경우 유효성 검사
      if (typeof value === "number") {
        if (isNaN(value)) {
          processedValue = 0; // 기본값 설정
        }
      }

      onChange({
        fringeFactor: defaultFringeFactor,
        count: defaultCount,
        enabled: defaultEnabled,
        [field]: processedValue,
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
              value={defaultFringeFactor.toString()} // 문자열로 변환
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
              value={defaultCount.toString()} // 문자열로 변환
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

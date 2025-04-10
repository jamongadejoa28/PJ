// src/app/components/VehicleSettings.tsx
"use client";

interface VehicleData {
  fringeFactor: number | null;
  count: number | null;
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
  const handleChange = (field: keyof VehicleData, value: string | boolean) => {
    if (onChange) {
      let processedValue: number | boolean | null = value;

      // 문자열 값(숫자 입력)을 처리
      if (typeof value === "string") {
        // 빈 문자열이면 null로 설정
        if (value === "") {
          processedValue = null;
        } else {
          const numValue = parseFloat(value);
          // 숫자가 유효하고 범위 내에 있는지 확인
          if (!isNaN(numValue)) {
            if (field === "count") {
              processedValue = numValue;
            } else if (field === "fringeFactor") {
              processedValue = numValue;
            }
          } else {
            return; // 유효하지 않은 숫자면 업데이트하지 않음
          }
        }
      }

      onChange({
        fringeFactor:
          field === "fringeFactor"
            ? typeof processedValue === "boolean"
              ? defaultFringeFactor
              : processedValue
            : defaultFringeFactor,
        count:
          field === "count"
            ? typeof processedValue === "boolean"
              ? defaultCount
              : processedValue
            : defaultCount,
        enabled: field === "enabled" ? Boolean(processedValue) : defaultEnabled,
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
              value={defaultFringeFactor ?? ""}
              onChange={(e) => handleChange("fringeFactor", e.target.value)}
              onKeyDown={(e) => {
                // 직접 입력은 제한 없이 허용
              }}
              onMouseDown={(e) => {
                // 버튼 클릭시에만 min/max 제한 적용
                const input = e.target as HTMLInputElement;
                input.min = "0.5";
                input.max = "100";
              }}
              onMouseUp={(e) => {
                // 버튼 조작이 끝나면 제한 해제
                const input = e.target as HTMLInputElement;
                input.removeAttribute("min");
                input.removeAttribute("max");
              }}
              step={0.1}
              className="number-input"
            />
          </label>
          <label className="option-label">
            Count
            <input
              type="number"
              value={defaultCount ?? ""}
              onChange={(e) => handleChange("count", e.target.value)}
              onKeyDown={(e) => {
                // 직접 입력은 제한 없이 허용
              }}
              onMouseDown={(e) => {
                // 버튼 클릭시에만 min/max 제한 적용
                const input = e.target as HTMLInputElement;
                input.min = "0.2";
                input.max = "100";
              }}
              onMouseUp={(e) => {
                // 버튼 조작이 끝나면 제한 해제
                const input = e.target as HTMLInputElement;
                input.removeAttribute("min");
                input.removeAttribute("max");
              }}
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

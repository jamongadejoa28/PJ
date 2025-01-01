// src/app/componenets/SpeeChart.tsx

"use client"

import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

interface WebSocketMessage {
  type: string;
  data: Array<{
    id: string;
    speed: number;
  }>;
}

interface WebSocketCustomEvent extends CustomEvent {
  detail: WebSocketMessage;
}

interface SpeedChartProps {
  simulationActive: boolean;
}

const SpeedChart: React.FC<SpeedChartProps> = ({ simulationActive }) => {
  // 시계열 속도 데이터를 저장하는 상태
  const [speedData, setSpeedData] = useState<Array<{ time: number; speed: number }>>([]);
  // 현재 평균 속도를 저장하는 상태
  const [currentSpeed, setCurrentSpeed] = useState<number>(0);
  
  useEffect(() => {
    // 시뮬레이션이 비활성화되면 데이터 초기화
    if (!simulationActive) {
      setSpeedData([]);
      setCurrentSpeed(0);
      return;
    }

    // WebSocket 메시지 핸들러
    const handleSpeedUpdate = (event: WebSocketCustomEvent) => {
      try {
        const message = event.detail;
        // vehicle_positions 메시지를 받으면 해당 시점의 모든 차량 속도 평균을 계산
        if (message.type === "vehicle_positions" && Array.isArray(message.data)) {
          // data 배열에 있는 차량들이 곧 현재 지도에 표시되는 차량들입니다
          // 이 배열에 있는 차량의 속도만 사용하여 평균을 계산합니다
          const totalSpeed = message.data.reduce((sum, vehicle) => sum + vehicle.speed, 0);
          const averageSpeed = message.data.length > 0 ? totalSpeed / message.data.length : 0;
          
          // 현재 속도 업데이트
          setCurrentSpeed(averageSpeed);
          
          // 시계열 데이터 업데이트
          setSpeedData(prev => {
            const time = prev.length * 1; // 1초 간격으로 시간 증가
            const newData = [...prev, { time, speed: averageSpeed }];
            // 최근 60초 데이터만 유지
            return newData.slice(-60);
          });
        }
      } catch (error) {
        console.error("속도 데이터 처리 중 오류:", error);
      }
    };

    window.addEventListener('websocketMessage', handleSpeedUpdate as EventListener);
    return () => {
      window.removeEventListener('websocketMessage', handleSpeedUpdate as EventListener);
    };
  }, [simulationActive]);

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-slate-800 border border-slate-700 px-4 py-2 rounded-lg shadow-xl">
          <p className="text-slate-300 font-medium mb-1">Time: {label}s</p>
          <p className="text-emerald-400 text-lg font-bold">
            {payload[0].value.toFixed(1)} km/h
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="container mt-5">
      <div className="bg-slate-900 rounded-xl shadow-2xl p-6 relative overflow-hidden">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h4 className="text-xl font-bold text-slate-100 mb-1">실시간 평균 차량 속도</h4>
            <div className="flex items-center gap-3">
              <div className="text-3xl font-bold text-emerald-400">
                {currentSpeed.toFixed(1)}
                <span className="text-lg text-emerald-500 ml-1">km/h</span>
              </div>
              {simulationActive && (
                <div className="flex items-center">
                  <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse mr-2"></div>
                  <span className="text-slate-400 text-sm">실시간</span>
                </div>
              )}
            </div>
          </div>
        </div>
        
        <div className="h-[200px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={speedData}
              margin={{ top: 10, right: 10, left: 0, bottom: 10 }}
            >
              <defs>
                <linearGradient id="speedGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#059669" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#059669" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid 
                strokeDasharray="3 3" 
                stroke="#1e293b" 
                vertical={false}
              />
              <XAxis
                dataKey="time"
                stroke="#64748b"
                tick={{ fill: '#64748b', fontSize: 12 }}
                axisLine={{ stroke: '#334155' }}
              />
              <YAxis
                stroke="#64748b"
                tick={{ fill: '#64748b', fontSize: 12 }}
                axisLine={{ stroke: '#334155' }}
                tickLine={{ stroke: '#334155' }}
              />
              <Tooltip content={<CustomTooltip />} />
              <Line
                type="monotone"
                dataKey="speed"
                stroke="#10b981"
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
              <Line
                type="monotone"
                dataKey="speed"
                stroke="transparent"
                fill="url(#speedGradient)"
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {!simulationActive && speedData.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center bg-slate-900/90 backdrop-blur-sm">
            <div className="text-center">
              <p className="text-slate-400 text-sm">
                시뮬레이션이 시작되면 실시간 속도 그래프가 표시됩니다
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SpeedChart;
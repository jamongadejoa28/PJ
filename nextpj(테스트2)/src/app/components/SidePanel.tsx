// src/app/components/SidePanel.tsx
"use client";

import React, { useState } from "react";
import Options from "./Options";
import Vehicles from "./Vehicles";
import RoadTypes from "./RoadTypes";
import GenerateButton from "./GenerateButton";
import Copyright from "./Copyright";
import { useMap } from "../context/MapContext";

const SidePanel = () => {
  const [activeTab, setActiveTab] = useState<number | null>(0);
  const { setView } = useMap(); // MapContext 사용

  const toggleTab = (id: number) => {
    if (activeTab === id) {
      setActiveTab(null);
    } else {
      setActiveTab(id);
    }
  };

  return (
    <div
      className={`fixed top-0 right-0 w-64 h-full bg-indigo-700 text-white transition-transform duration-200 ${
        activeTab !== null ? "translate-x-0" : "translate-x-full"
      }`}
    >
      <div className="flex flex-col h-full">
        {/* 탭 버튼들 */}
        <div className="flex flex-col">
          <Tab
            id={0}
            title="Options"
            icon="/images/generate.png"
            activeTab={activeTab}
            toggleTab={toggleTab}
          />
          <Tab
            id={1}
            title="Vehicles"
            icon="/images/passenger.png"
            activeTab={activeTab}
            toggleTab={toggleTab}
          />
          <Tab
            id={2}
            title="Road-Types"
            icon="/images/road.png"
            activeTab={activeTab}
            toggleTab={toggleTab}
          />
          <Tab
            id={3}
            title="Copyright"
            symbol="©"
            activeTab={activeTab}
            toggleTab={toggleTab}
          />
        </div>
        {/* 탭 내용 */}
        <div className="flex-1 overflow-y-auto">
          {activeTab === 0 && <Options />}
          {activeTab === 1 && <Vehicles />}
          {activeTab === 2 && <RoadTypes />}
          {activeTab === 3 && <Copyright />}
        </div>
      </div>
      {/* Generate Scenario 버튼은 항상 사이드 패널 위에 고정 */}
      {activeTab !== null && <GenerateButton />}
    </div>
  );
};

type TabProps = {
  id: number;
  title: string;
  icon?: string;
  symbol?: string;
  activeTab: number | null;
  toggleTab: (id: number) => void;
};

const Tab: React.FC<TabProps> = ({
  id,
  title,
  icon,
  symbol,
  activeTab,
  toggleTab,
}) => {
  return (
    <div
      className={`tab p-4 cursor-pointer ${
        activeTab === id ? "bg-indigo-800" : "bg-indigo-700 hover:bg-indigo-600"
      }`}
      onClick={() => toggleTab(id)}
      title={title}
    >
      {icon && (
        <img src={icon} alt={`${title} icon`} className="w-8 h-8 mx-auto" />
      )}
      {symbol && <span className="block text-center">{symbol}</span>}
    </div>
  );
};

export default SidePanel;

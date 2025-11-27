import React from 'react';
import { Laptop, Wifi, WifiOff } from 'lucide-react';
import { Link } from 'react-router-dom';
import { createPageUrl } from '@/utils';

export default function DeviceStatusCard({ status = 'connected' }) {
  // Status: 'setup' | 'offline' | 'connected'
  
  const config = {
    setup: {
      icon: Laptop,
      text: '连接设备',
      subtext: '进行初始化设置',
      color: 'bg-[#007AFF]',
      textColor: 'text-white',
      iconBg: 'bg-white/20',
      link: 'DeviceSetup',
      disabled: false
      },
      offline: {
      icon: WifiOff,
      text: '设备离线',
      subtext: '正在持续搜索设备...',
      color: 'bg-[#E5E5EA]',
      textColor: 'text-[#8E8E93]',
      iconBg: 'bg-white',
      link: null,
      disabled: true
    },
    connected: {
      icon: Wifi,
      text: '设备管理',
      subtext: '设备已连接',
      color: 'bg-white border border-[#E5E5EA] shadow-sm',
      textColor: 'text-black',
      iconBg: 'bg-[#F2F2F7]',
      link: 'DeviceManager',
      disabled: false
    }
  };

  const activeConfig = config[status];
  const Icon = activeConfig.icon;

  const Content = (
    <div className={`p-5 rounded-xl flex items-center gap-4 transition-all ${activeConfig.color} ${activeConfig.disabled ? 'opacity-70 cursor-not-allowed' : 'active:scale-[0.98]'}`}>
      <div className={`p-3 rounded-lg backdrop-blur-sm ${activeConfig.iconBg}`}>
        <Icon className={`w-6 h-6 ${activeConfig.textColor === 'text-white' ? 'text-white' : 'text-[#007AFF]'}`} />
      </div>
      <div>
        <div className={`font-bold text-lg leading-tight ${activeConfig.textColor}`}>{activeConfig.text}</div>
        <div className={`text-xs mt-1 ${activeConfig.textColor === 'text-white' ? 'text-white/80' : 'text-[#8E8E93]'}`}>{activeConfig.subtext}</div>
      </div>
    </div>
  );

  if (activeConfig.link && !activeConfig.disabled) {
    return <Link to={createPageUrl(activeConfig.link)}>{Content}</Link>;
  }

  return <div>{Content}</div>;
}
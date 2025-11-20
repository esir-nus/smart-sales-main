import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { createPageUrl } from '@/utils';
import { Smartphone, Wifi, CheckCircle, Loader2, ArrowRight } from 'lucide-react';
import NeumorphicButton from '@/components/ui/NeumorphicButton';

export default function DeviceSetupPage() {
  const navigate = useNavigate();
  // States: scanning, found, wifi_input, pairing, success
  const [step, setStep] = useState('scanning');
  const [wifiSSID, setWifiSSID] = useState('');
  const [wifiPassword, setWifiPassword] = useState('');
  
  useEffect(() => {
    // Step 1: Auto start scanning
    if (step === 'scanning') {
      const timer = setTimeout(() => {
        setStep('found');
      }, 3000); 
      return () => clearTimeout(timer);
    }
  }, [step]);

  const handleProceedToWifi = () => {
    setStep('wifi_input');
  };

  const handleWifiConnect = () => {
    setStep('pairing');
    // Simulate network connection and pairing
    setTimeout(() => {
      setStep('success');
      setTimeout(() => {
        navigate(createPageUrl('DeviceManager'));
      }, 2000);
    }, 3000);
  };

  return (
    <div className="min-h-[80vh] flex flex-col items-center justify-center p-6 relative">
      
      {/* 3D Device Placeholder Animation */}
      <div className="relative w-64 h-64 mb-12 flex items-center justify-center">
        {/* Ripples for scanning */}
        {step === 'scanning' && (
          <>
            <motion.div 
              animate={{ scale: [1, 2], opacity: [0.5, 0] }}
              transition={{ duration: 2, repeat: Infinity, ease: "easeOut" }}
              className="absolute inset-0 border border-[#007AFF] rounded-full"
            />
            <motion.div 
              animate={{ scale: [1, 2], opacity: [0.5, 0] }}
              transition={{ duration: 2, repeat: Infinity, ease: "easeOut", delay: 1 }}
              className="absolute inset-0 border border-[#007AFF] rounded-full"
            />
          </>
        )}

        {/* Device Icon/Model */}
        <motion.div 
          animate={step === 'scanning' ? { rotate: [0, 10, -10, 0] } : {}}
          transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
          className="relative z-10 w-40 h-40 bg-white rounded-3xl shadow-xl flex items-center justify-center border border-[#E5E5EA]"
        >
           {step === 'success' ? (
             <CheckCircle className="w-20 h-20 text-[#34C759]" />
           ) : (
             <Smartphone className={`w-20 h-20 ${step === 'found' ? 'text-[#007AFF]' : 'text-[#8E8E93]'}`} />
           )}
           
           {/* Status Indicator Dot */}
           <div className={`absolute top-4 right-4 w-3 h-3 rounded-full ${
             step === 'success' ? 'bg-[#34C759]' : 
             step === 'found' ? 'bg-[#007AFF]' : 'bg-orange-400'
           }`} />
        </motion.div>
      </div>

      {/* Text and Actions Area */}
      <div className="w-full max-w-md text-center space-y-6 min-h-[160px]">
        <AnimatePresence mode="wait">
          
          {step === 'scanning' && (
            <motion.div
              key="scanning"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              <h2 className="text-2xl font-bold text-black mb-2">正在搜索设备...</h2>
              <p className="text-[#8E8E93]">请确保设备已开启蓝牙并靠近手机</p>
            </motion.div>
          )}

          {step === 'found' && (
            <motion.div
              key="found"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              <h2 className="text-2xl font-bold text-black mb-2">发现新设备</h2>
              <p className="text-[#8E8E93] mb-8">Smart Assistant X1</p>
              <NeumorphicButton 
                onClick={handleProceedToWifi} 
                variant="primary"
                className="w-full py-4 text-lg bg-[#007AFF] text-white shadow-lg shadow-blue-200"
              >
                配置网络
              </NeumorphicButton>
            </motion.div>
          )}

          {step === 'wifi_input' && (
            <motion.div
              key="wifi_input"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="w-full text-left"
            >
              <h2 className="text-xl font-bold text-black mb-1 text-center">连接 Wi-Fi</h2>
              <p className="text-[#8E8E93] text-sm mb-6 text-center">请将设备连接至同一网络</p>
              
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-[#E5E5EA] space-y-4">
                <div>
                  <label className="block text-xs font-bold text-[#8E8E93] mb-1.5 uppercase">Wi-Fi 名称</label>
                  <div className="relative">
                    <Wifi className="absolute left-3 top-3 w-4 h-4 text-[#8E8E93]" />
                    <input 
                      type="text" 
                      value={wifiSSID}
                      onChange={(e) => setWifiSSID(e.target.value)}
                      placeholder="输入 Wi-Fi 名称"
                      className="w-full pl-9 pr-4 py-2.5 bg-[#F2F2F7] rounded-xl text-black text-sm outline-none focus:ring-2 ring-[#007AFF]/20 transition-all"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#8E8E93] mb-1.5 uppercase">密码</label>
                  <input 
                    type="password" 
                    value={wifiPassword}
                    onChange={(e) => setWifiPassword(e.target.value)}
                    placeholder="输入 Wi-Fi 密码"
                    className="w-full px-4 py-2.5 bg-[#F2F2F7] rounded-xl text-black text-sm outline-none focus:ring-2 ring-[#007AFF]/20 transition-all"
                  />
                </div>

                <NeumorphicButton 
                  onClick={handleWifiConnect}
                  disabled={!wifiSSID || !wifiPassword}
                  variant="primary"
                  className="w-full mt-4 py-3 text-sm font-bold"
                >
                  连接设备
                </NeumorphicButton>
              </div>
            </motion.div>
          )}

          {step === 'pairing' && (
            <motion.div
              key="pairing"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              <div className="flex flex-col items-center">
                <Loader2 className="w-8 h-8 text-[#007AFF] animate-spin mb-4" />
                <h2 className="text-xl font-bold text-black">正在配对...</h2>
                <p className="text-[#8E8E93] text-sm mt-2">正在同步设备配置</p>
              </div>
            </motion.div>
          )}

          {step === 'success' && (
            <motion.div
              key="success"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              <h2 className="text-2xl font-bold text-black mb-2">连接成功!</h2>
              <p className="text-[#8E8E93]">正在跳转至设备管理页面...</p>
            </motion.div>
          )}

        </AnimatePresence>
      </div>
    </div>
  );
}
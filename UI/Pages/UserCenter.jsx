import React, { useEffect, useState } from 'react';
import { base44 } from '@/api/base44Client';
import { User, Settings, CreditCard, Shield, LogOut, ChevronRight, Smartphone } from 'lucide-react';
import { Link } from 'react-router-dom';
import { createPageUrl } from '@/utils';
import NeumorphicCard from '@/components/ui/NeumorphicCard';
import NeumorphicButton from '@/components/ui/NeumorphicButton';

export default function UserCenterPage() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const u = await base44.auth.me();
        setUser(u);
      } catch (e) {
        console.log("Guest user");
      }
    };
    loadUser();
  }, []);

  const handleLogout = async () => {
    await base44.auth.logout();
  };

  const MenuRow = ({ icon: Icon, title, subtitle, to, onClick, danger }) => {
    const content = (
      <div className={`flex items-center justify-between p-4 w-full ${danger ? 'text-red-500' : 'text-black'}`}>
        <div className="flex items-center gap-4">
          <div className={`w-10 h-10 rounded-2xl flex items-center justify-center ${danger ? 'bg-red-50' : 'bg-[#F2F2F7]'}`}>
            <Icon size={20} className={danger ? 'text-red-500' : 'text-[#007AFF]'} />
          </div>
          <div className="text-left">
            <div className="font-bold text-sm">{title}</div>
            {subtitle && <div className="text-xs text-[#8E8E93] mt-0.5">{subtitle}</div>}
          </div>
        </div>
        <ChevronRight size={18} className="text-[#C7C7CC]" />
      </div>
    );

    if (to) {
      return (
        <Link to={createPageUrl(to)} className="block mb-4">
          <NeumorphicCard className="p-0 hover:scale-[1.01] transition-transform bg-white border border-[#E5E5EA] shadow-sm rounded-2xl">
            {content}
          </NeumorphicCard>
        </Link>
      );
    }

    return (
      <button onClick={onClick} className="block w-full mb-4">
        <NeumorphicCard className="p-0 hover:scale-[1.01] transition-transform bg-white border border-[#E5E5EA] shadow-sm rounded-2xl">
          {content}
        </NeumorphicCard>
      </button>
    );
  };

  return (
    <div className="max-w-xl mx-auto pb-20">
      {/* Profile Header */}
      <div className="flex flex-col items-center mb-10 pt-4">
        <div className="w-24 h-24 rounded-full bg-white border border-[#E5E5EA] flex items-center justify-center mb-4 shadow-md">
          <User size={40} className="text-[#007AFF]" />
        </div>
        <h2 className="text-2xl font-bold text-black">{user?.full_name || '访客用户'}</h2>
        <p className="text-[#8E8E93]">{user?.email || '请登录'}</p>
        
        {!user && (
           <NeumorphicButton 
             onClick={() => base44.auth.redirectToLogin()} 
             className="mt-4 px-6 py-2 text-sm font-bold text-[#007AFF] bg-white border border-[#007AFF]"
           >
             登录
           </NeumorphicButton>
        )}
      </div>

      {/* Device Manager Entry */}
      <MenuRow 
        icon={Smartphone}
        title="设备管理"
        subtitle="管理已配对的设备与文件"
        to="DeviceManager"
      />

      <div className="h-px bg-[#E5E5EA] my-6 mx-4" />



       {/* Settings Group */}
       <div className="mb-2 px-2 text-xs font-bold text-[#8E8E93] uppercase tracking-wider">账户</div>
      
      <MenuRow 
        icon={CreditCard}
        title="订阅管理"
        subtitle="专业版 • 2024年10月续费"
      />
      
      <MenuRow 
        icon={Shield}
        title="隐私与安全"
        subtitle="密码, 双重认证, 数据控制"
      />

      <MenuRow 
        icon={Settings}
        title="通用设置"
        subtitle="语言, 通知"
      />

      {user && (
        <div className="mt-8">
          <MenuRow 
            icon={LogOut}
            title="退出登录"
            danger
            onClick={handleLogout}
          />
        </div>
      )}
    </div>
  );
}
import { useState } from 'react'
import { Header } from './components/Header'
import { HeroArea } from './components/HeroArea'
import { BottomDock } from './components/BottomDock'
import { RightToolbar } from './components/RightToolbar'
import { MobileFrame } from './components/MobileFrame'
import { HistoryPage } from './components/HistoryPage'
import { HistoryGlassLayer } from './components/HistoryGlassLayer'
import { HistoryCardLayout } from './components/HistoryCardLayout'
import { HistoryHybridLayout } from './components/HistoryHybridLayout'
import { PrototypeDashboard } from './components/PrototypeDashboard'
import * as SchedulerDrawer from './components/SchedulerDrawer'
import { clsx } from 'clsx'

type View = 'home' | 'history' | 'history_glass' | 'history_cards' | 'history_hybrid' | 'scheduler';

function App() {
  const [theme] = useState('theme-glass-sleek')
  const [currentView, setCurrentView] = useState<View>('home')

  const handleNavigate = (destination: string) => {
    setCurrentView(destination as View)
  }

  return (
    <div className="min-h-screen bg-[#1a1a1a] flex items-center justify-center pl-64 transition-all duration-300">
      {/* Dashboard Sidebar */}
      <PrototypeDashboard onNavigate={handleNavigate} currentView={currentView} />

      {/* Mobile Frame (Centered) */}
      <MobileFrame>
        <div className={clsx("w-full h-full relative font-sans antialiased text-prism-primary bg-prism-bg", theme)}>
          {/* Background Haze */}
          <div className="absolute top-[-20%] left-[-20%] w-[140%] h-[140%] bg-[radial-gradient(circle_at_50%_0%,rgba(120,119,198,0.1),transparent_70%)] pointer-events-none" />
          
          {/* Home View */}
          {currentView === 'home' && (
            <>
              <Header />
              <HeroArea />
              <RightToolbar />
              <BottomDock />
            </>
          )}
          
          {currentView === 'history' && (
            <HistoryPage />
          )}

          {currentView === 'history_glass' && (
            <HistoryGlassLayer />
          )}

          {currentView === 'history_cards' && (
            <HistoryCardLayout />
          )}

          {/* History View - Hybrid Variant */}
          {currentView === 'history_hybrid' && (
            <HistoryHybridLayout />
          )}

          {/* Scheduler View */}
          {currentView === 'scheduler' && (
            <div className="w-full h-full relative">
               {/* HACK: We render SchedulerDrawer directly. In real app, it might be an overlay. 
                   For prototype, we treat it as a page. */}
               <div className="absolute inset-0 bg-black/40 backdrop-blur-sm z-40" /> {/* Scrim */}
               <div className="absolute inset-x-0 bottom-0 h-[85vh] z-50 transform transition-transform duration-300 translate-y-0">
                  <SchedulerDrawer.SchedulerDrawer isOpen={true} onClose={() => setCurrentView('home')} />
               </div>
            </div>
          )}
        </div>
      </MobileFrame>
    </div>
  )
}

export default App

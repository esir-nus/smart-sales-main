import React, { useState } from 'react';
import { AnimatePresence } from 'framer-motion';
import { WelcomeStep, PermissionStep, HandshakeStep } from './Steps_1_3';
import { WakeStep, ScanStep, FoundStep } from './Steps_4_6';
import { FwCheckStep, WifiStep, NamingStep } from './Steps_7_9';
import { AccountStep, ProfileStep, CompleteStep } from './Steps_10_12';

interface OnboardingWizardProps {
    onComplete: () => void;
}

export const OnboardingWizard: React.FC<OnboardingWizardProps> = ({ onComplete }) => {
    const [step, setStep] = useState(1);

    const next = () => setStep(s => s + 1);

    const renderStep = () => {
        switch (step) {
            case 1: return <WelcomeStep onNext={next} />;
            case 2: return <PermissionStep onNext={next} />;
            case 3: return <HandshakeStep onNext={next} />;
            case 4: return <WakeStep onNext={next} />;
            case 5: return <ScanStep onNext={next} />;
            case 6: return <FoundStep onNext={next} />; // Manual Gate
            case 7: return <WifiStep onNext={next} />;
            case 8: return <FwCheckStep onNext={next} />;
            case 9: return <NamingStep onNext={next} />;
            case 10: return <AccountStep onNext={next} />;
            case 11: return <ProfileStep onNext={next} />;
            case 12: return <CompleteStep onComplete={onComplete} />;
            default: return null;
        }
    };

    return (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center p-6 bg-slate-900 text-white overflow-hidden font-sans">
            {/* Ambient Background */}
            <div className="absolute top-[-20%] left-[-20%] w-[140%] h-[140%] bg-gradient-to-br from-blue-900/40 via-purple-900/30 to-slate-900 blur-3xl -z-10 animate-pulse" />
            
            {/* Progress Indicator (Skip on Welcome & Complete) */}
            {step > 1 && step < 12 && (
                <div className="absolute top-8 w-64 h-1 bg-white/10 rounded-full overflow-hidden">
                    <div className="h-full bg-blue-500 transition-all duration-500 ease-out" style={{ width: `${((step - 1) / 11) * 100}%` }} />
                </div>
            )}

            <AnimatePresence mode="wait">
                {renderStep()}
            </AnimatePresence>
            
            {/* Version Watermark */}
            <div className="absolute bottom-6 text-[10px] text-white/20 font-mono tracking-widest">
                PRISM V15 FULL SPECTRUM • STEP {step}/12
            </div>
        </div>
    );
};

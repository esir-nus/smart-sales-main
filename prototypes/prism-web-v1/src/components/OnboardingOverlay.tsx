import React from 'react';
import { OnboardingWizard } from './onboarding/Wizard';

interface OnboardingOverlayProps {
    onComplete: () => void;
}

export const OnboardingOverlay: React.FC<OnboardingOverlayProps> = ({ onComplete }) => {
    return <OnboardingWizard onComplete={onComplete} />;
};


import React, { useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Mic, Send } from 'lucide-react';
import { clsx } from 'clsx';

interface Message {
    id: string;
    role: 'ai' | 'user';
    text: string;
}

interface ChatExpandedViewProps {
    initialMessage: string;
    onSend?: (text: string) => void;
    children?: React.ReactNode; // For any custom content above chat if needed
}

export const ChatExpandedView: React.FC<ChatExpandedViewProps> = ({ initialMessage, onSend, children }) => {
    const [messages, setMessages] = React.useState<Message[]>([
        { id: 'init', role: 'ai', text: initialMessage }
    ]);
    const [inputValue, setInputValue] = React.useState('');
    const scrollRef = useRef<HTMLDivElement>(null);

    // Auto-scroll to bottom
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [messages]);

    const handleSend = () => {
        if (!inputValue.trim()) return;
        
        const newMsg: Message = { 
            id: Date.now().toString(), 
            role: 'user', 
            text: inputValue 
        };
        
        setMessages(prev => [...prev, newMsg]);
        setInputValue('');
        
        if (onSend) {
            onSend(inputValue);
        }

        // Simulate AI reply for demo purposes (if no real backend)
        // In a real app, this would be handled by the parent or a hook
        setTimeout(() => {
             setMessages(prev => [...prev, {
                 id: (Date.now() + 1).toString(),
                 role: 'ai',
                 text: "好的，已更新。" // "Okay, updated."
             }]);
        }, 1000);
    };

    return (
        <div className="flex flex-col h-[300px] w-full"> {/* Fixed height for consistency */}
            
            {/* Scrollable Chat Area */}
            <div 
                ref={scrollRef}
                className="flex-1 overflow-y-auto px-4 py-2 space-y-4 no-scrollbar"
            >
                {children && <div className="mb-4">{children}</div>}

                {messages.map((msg) => (
                    <motion.div 
                        key={msg.id}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className={clsx(
                            "flex flex-col max-w-[85%]",
                            msg.role === 'user' ? "ml-auto items-end" : "mr-auto items-start"
                        )}
                    >
                        <div className={clsx(
                            "px-1 py-1 text-sm leading-relaxed text-prism-primary max-w-full",
                            msg.role === 'ai' 
                                ? "bg-transparent font-medium" 
                                : "bg-prism-accent text-white px-3 py-2.5 rounded-2xl rounded-tr-none shadow-sm w-fit"
                        )}>
                            {msg.text}
                        </div>
                    </motion.div>
                ))}
            </div>

            {/* Input Footer */}
            <div className="p-3 bg-black/5 border-t border-prism-border mt-2">
                <div className="flex items-center gap-2 bg-prism-surface p-1 rounded-full border border-prism-border shadow-sm focus-within:border-prism-accent/50 focus-within:ring-2 focus-within:ring-prism-accent/20 transition-all">
                    <input 
                        type="text" 
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        placeholder="回复..." 
                        className="flex-1 bg-transparent border-none outline-none text-sm px-3 text-prism-primary placeholder-prism-secondary h-9"
                        onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                        onClick={(e) => e.stopPropagation()} // Prevent card collapse
                    />
                    
                    {inputValue ? (
                        <button 
                            onClick={(e) => { e.stopPropagation(); handleSend(); }}
                            className="w-9 h-9 bg-prism-accent rounded-full text-white flex items-center justify-center hover:scale-105 active:scale-95 transition-all shadow-md"
                        >
                            <Send size={16} />
                        </button>
                    ) : (
                        <button 
                            className="w-9 h-9 bg-black/5 rounded-full text-prism-secondary flex items-center justify-center hover:bg-black/10 transition-all"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <Mic size={18} />
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

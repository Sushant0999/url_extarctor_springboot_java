import React, { useEffect, useState } from 'react';
import { BiLinkExternal } from 'react-icons/bi';
import { motion } from 'framer-motion';
import { getHealth } from '../apis/Actuator';

export default function Nav() {
  const [isUp, setIsUp] = useState(false);

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const health = await getHealth();
        setIsUp(health.status === 'UP');
      } catch (error) {
        setIsUp(false);
      }
    };

    checkHealth();
    const interval = setInterval(checkHealth, 30000); // Check every 30s
    return () => clearInterval(interval);
  }, []);

  return (
    <nav 
      className="fixed top-0 z-[100] w-full glass-effect border-b border-white/5 backdrop-blur-[30px] saturate-[200%]"
    >
      <div className="max-w-[1280px] mx-auto px-4 md:px-8">
        <div className="h-16 flex items-center justify-between">
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          >
            <div 
              className="flex items-center gap-4 cursor-pointer" 
              onClick={() => window.location.href='/'}
            >
              <div 
                className="bg-gradient-to-br from-[#818cf8] to-[#6366f1] p-1.5 rounded-xl shadow-[0_4px_15px_rgba(129,140,248,0.3)]"
              >
                <BiLinkExternal className="text-white w-5 h-5" />
              </div>
              <span className="text-lg font-[800] tracking-tight text-white">
                URL<span className="bg-gradient-to-r from-[#818cf8] to-[#10b981] bg-clip-text text-transparent">X</span>
              </span>
            </div>
          </motion.div>

          <div className="hidden md:flex items-center gap-10">
            {[
              { label: 'Engine', path: '/' },
              { label: 'API', path: '/actuator' }
            ].map((item) => (
              <a
                key={item.label}
                href={`#${item.path}`}
                className="text-[10px] font-[700] text-gray-400 tracking-[0.1em] uppercase hover:text-white transition-all duration-300 no-underline"
              >
                {item.label}
              </a>
            ))}
          </div>

          <motion.div 
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            className="flex items-center gap-3 px-4 py-1.5 rounded-full border border-white/5 bg-white/[0.02] backdrop-blur-md"
          >
            <div className="relative flex items-center">
              <div className={`w-2 h-2 rounded-full ${isUp ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.6)]' : 'bg-rose-500 shadow-[0_0_8px_rgba(244,63,94,0.6)]'}`} />
              {isUp && (
                <div className="absolute w-2 h-2 rounded-full bg-emerald-400 animate-ping opacity-75" />
              )}
            </div>
            <span className="text-[10px] font-[800] tracking-[0.15em] uppercase text-gray-400 whitespace-nowrap">
              {isUp ? 'Live Server' : 'Server Down'}
            </span>
          </motion.div>
        </div>
      </div>
    </nav>
  );
}

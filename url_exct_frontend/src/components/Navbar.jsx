import { BiLinkExternal } from 'react-icons/bi';
import { motion } from 'framer-motion';

export default function Nav() {
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
              <span 
                className="text-lg font-[800] tracking-tight text-white"
              >
                URL<span className="bg-gradient-to-r from-[#818cf8] to-[#10b981] bg-clip-text text-transparent">X</span>
              </span>
            </div>
          </motion.div>

          <div className="hidden md:flex items-center gap-10">
            {['Engine', 'Features', 'API'].map((item) => (
              <a
                key={item}
                href="#"
                className="text-[10px] font-[700] text-gray-400 tracking-[0.1em] uppercase hover:text-white transition-all duration-300 no-underline"
              >
                {item}
              </a>
            ))}
          </div>

          <div />
        </div>
      </div>
    </nav>
  );
}

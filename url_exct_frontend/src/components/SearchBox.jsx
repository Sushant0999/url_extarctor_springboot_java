import React, { useEffect, useState, useRef } from 'react';
import { addLinks } from '../apis/AddLinks';
import { getResult } from '../apis/GetResult';
import { ScatterBoxLoaderComponent } from './loaders/ScatterBoxLoaderComponent';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

const steps = [
    "Initializing task queue...",
    "Connecting to extraction engine...",
    "Analyzing page metadata...",
    "Scanning for media content...",
    "Extracting links and assets...",
    "Finalizing data package..."
];

// Simple toast replacement using state
function useSimpleToast() {
    const [toast, setToast] = useState(null);
    const show = ({ title, description, status = 'info', duration = 3000 }) => {
        setToast({ title, description, status });
        setTimeout(() => setToast(null), duration);
    };
    return { toast, show };
}

export default function SearchBox() {
    const navigate = useNavigate();
    const [inputText, setInputText] = useState('');
    const [isLoading, setLoading] = useState(false);
    const [enable, isEnable] = useState(true);
    const [currentStep, setCurrentStep] = useState(0);
    const [taskId, setTaskId] = useState(null);
    const eventSourceRef = useRef(null);
    const { toast, show: showToast } = useSimpleToast();

    useEffect(() => {
        isEnable(true);
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);

    const handleSearch = async () => {
        if (!inputText) {
            showToast({ title: 'Enter a URL to analyze', status: 'info' });
            return;
        }

        setLoading(true);
        setCurrentStep(0);

        try {
            const formatUrl = (url) => {
                let trimmed = url.trim();
                if (!trimmed) return "";
                if (!/^https?:\/\//i.test(trimmed)) trimmed = `https://${trimmed}`;
                return trimmed;
            };

            const urls = inputText.split(/[,\n]/).map(u => formatUrl(u)).filter(u => u.length > 0);

            if (urls.length === 0) { setLoading(false); return; }

            const taskIdMap = await addLinks(urls.length > 1 ? urls : urls[0]);
            const ids = Object.values(taskIdMap);
            const taskCount = ids.length;

            if (taskCount > 1) showToast({ title: `Processing ${taskCount} URLs`, status: 'info' });
            if (!ids || ids.length === 0) throw new Error("No taskId received");

            setTaskId(taskCount > 1 ? 'BULK_PROCESSING' : ids[0]);
            
            startPolling(ids);

        } catch (error) {
            setLoading(false);
            console.error(error);
            showToast({ title: 'Request failed', description: error.message, status: 'error' });
        }
    };

    const startPolling = async (ids) => {
        const { getBulkStatus } = await import('../apis/GetStatus');
        const { getBulkResults } = await import('../apis/GetResult');

        const stepInterval = setInterval(() => {
            setCurrentStep(prev => prev < steps.length - 2 ? prev + 1 : prev);
        }, 2000);

        const pollInterval = setInterval(async () => {
            try {
                const statusMap = await getBulkStatus(ids);
                
                const allFinished = ids.every(id => statusMap[id] === 'COMPLETED' || statusMap[id] === 'FAILED');
                const anyFailed = ids.some(id => statusMap[id] === 'FAILED');

                if (allFinished) {
                    clearInterval(pollInterval);
                    clearInterval(stepInterval);
                    setCurrentStep(steps.length - 1);
                    
                    if (anyFailed && ids.length === 1) {
                        setLoading(false);
                        showToast({ title: 'Extraction failed', status: 'error' });
                        return;
                    }

                    const completedIds = ids.filter(id => statusMap[id] === 'COMPLETED');
                    
                    if (completedIds.length === 0) {
                        setLoading(false);
                        showToast({ title: 'All extractions failed', status: 'error' });
                        return;
                    }

                    const bulkData = await getBulkResults(completedIds);
                    const finalDataArray = completedIds.map(id => {
                        const data = bulkData[id];
                        data.taskId = id;
                        return data;
                    });

                    localStorage.setItem('lastExtraction', JSON.stringify(finalDataArray));
                    setTimeout(() => navigate('/result'), 1000);
                }
            } catch (err) {
                console.error("Polling error:", err);
            }
        }, 2000);
    };

    const statusColors = { info: 'bg-indigo-500/80', error: 'bg-red-500/80', success: 'bg-emerald-500/80' };
    const progressPct = Math.round(((currentStep + 1) / steps.length) * 100);

    return (
        <div className="min-h-[60vh] flex flex-col justify-center items-center px-4">
            {/* Simple toast notification */}
            {toast && (
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0 }}
                    className={`fixed top-24 right-6 z-[200] px-5 py-3 rounded-2xl text-white text-sm font-semibold shadow-2xl ${statusColors[toast.status] || 'bg-indigo-500/80'} backdrop-blur-md border border-white/10`}
                >
                    <p className="font-bold text-xs tracking-wide uppercase">{toast.title}</p>
                    {toast.description && <p className="text-white/70 text-xs mt-0.5">{toast.description}</p>}
                </motion.div>
            )}

            <AnimatePresence mode="wait">
                {isLoading ? (
                    <motion.div
                        key="loader"
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 1.05 }}
                        className="w-full max-w-md"
                    >
                        <div className="glass-effect flex flex-col items-center gap-8 p-12 rounded-[32px] border border-indigo-400/20">
                            <ScatterBoxLoaderComponent />
                            <div className="flex flex-col items-center gap-4 w-full">
                                <p className="text-lg font-semibold text-indigo-200 tracking-wide text-center">
                                    {steps[currentStep]}
                                </p>
                                {taskId && (
                                    <span className="text-[9px] font-bold tracking-widest uppercase bg-purple-500/20 text-purple-300 border border-purple-500/30 px-3 py-1 rounded-full">
                                        TRACKING: {taskId.substring(0, 8)}...
                                    </span>
                                )}
                                {/* Progress bar */}
                                <div className="w-full bg-white/5 rounded-full h-1 overflow-hidden">
                                    <motion.div
                                        className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full"
                                        initial={{ width: 0 }}
                                        animate={{ width: `${progressPct}%` }}
                                        transition={{ duration: 0.5 }}
                                    />
                                </div>
                                <div className="flex justify-between w-full">
                                    <span className="text-[10px] font-bold tracking-widest uppercase bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded">
                                        STEP {currentStep + 1}
                                    </span>
                                    <span className="text-xs font-bold text-indigo-300">{progressPct}%</span>
                                </div>
                            </div>
                        </div>
                    </motion.div>
                ) : (
                    <motion.div
                        key="search"
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95 }}
                        className="w-full max-w-2xl"
                        transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
                    >
                        <div className="glass-effect flex flex-col gap-8 p-10 md:p-12 rounded-[40px] border border-white/8">
                            {/* Header text */}
                            <div className="flex flex-col items-center gap-4 text-center">
                                <span className="text-[10px] font-bold tracking-[0.2em] uppercase border border-emerald-500/40 text-emerald-400 px-3 py-1 rounded-full">
                                    NEXT GEN EXTRACTION
                                </span>
                                <h1 className="text-5xl md:text-6xl font-black text-gradient tracking-tight leading-none">
                                    Deep Web Analyzer
                                </h1>
                                <p className="text-gray-400 text-lg max-w-md font-medium">
                                    Synthesize structured data, visual assets, and SEO insights from any URL.
                                </p>
                            </div>

                            {/* Input section */}
                            <div className="flex flex-col gap-6 w-full">
                                <div className="relative w-full">
                                    <span className="absolute -top-3 right-6 z-10 text-[9px] font-black tracking-widest bg-indigo-600 text-white px-2 py-0.5 rounded-lg uppercase">
                                        ENTER ONE OR MORE URLS
                                    </span>
                                    <textarea
                                        placeholder="google.com, github.com (or one per line)"
                                        value={inputText}
                                        onChange={(e) => setInputText(e.target.value)}
                                        rows={Math.min(6, Math.max(2, inputText.split(/\n|,/).length))}
                                        className="w-full px-8 pt-8 pb-6 bg-white/5 border border-white/10 rounded-3xl text-base text-gray-100 font-semibold outline-none transition-all duration-300 resize-none font-[inherit] placeholder:text-gray-600 focus:border-indigo-400 focus:shadow-[0_0_30px_rgba(129,140,248,0.1)] focus:bg-white/8"
                                    />
                                </div>

                                <div className="flex flex-wrap items-center justify-between gap-4">
                                    {/* JS Render Toggle */}
                                    <div className="flex items-center gap-3 bg-white/5 px-4 py-3 rounded-xl border border-white/5">
                                        <div className="flex flex-col">
                                            <span className="text-[10px] font-black tracking-[0.1em] text-indigo-300 uppercase">JS RENDER</span>
                                            <span className="text-[8px] text-gray-500 uppercase tracking-wider">HEADLESS ENGINE</span>
                                        </div>
                                        {/* Custom toggle */}
                                        <button
                                            onClick={() => isEnable(!enable)}
                                            className={`relative w-9 h-5 rounded-full transition-colors duration-300 ${enable ? 'bg-indigo-500' : 'bg-white/10'}`}
                                        >
                                            <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform duration-300 ${enable ? 'translate-x-4' : 'translate-x-0'}`} />
                                        </button>
                                    </div>

                                    {/* Extract button */}
                                    <button
                                        onClick={handleSearch}
                                        className="h-[68px] px-12 bg-gradient-to-r from-[#818cf8] to-[#6366f1] text-white rounded-3xl text-sm font-[900] tracking-[0.05em] shadow-[0_10px_40px_-10px_rgba(99,102,241,0.5)] hover:-translate-y-1 hover:shadow-[0_15px_50px_-10px_rgba(99,102,241,0.6)] active:translate-y-0 transition-all duration-300"
                                    >
                                        INITIATE EXTRACTION
                                    </button>
                                </div>
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

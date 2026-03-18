import React, { useEffect, useState } from 'react'
import Navbar from '../components/Navbar'
import LinkCard from '../components/cards/LinkCard'
import ImageCard from '../components/cards/ImageCard'
import TextCard from '../components/cards/TextCard'
import SummaryCard from '../components/analysis/SummaryCard'
import SeoAuditCard from '../components/analysis/SeoAuditCard'
import TechStackCard from '../components/analysis/TechStackCard'
import { useNavigate } from 'react-router-dom'
import DownloadFileComponent from '../components/DownloadToast'
import { BiArrowBack, BiScreenshot, BiData, BiShieldQuarter } from 'react-icons/bi'
import { motion } from 'framer-motion'

const tabs = [
    { name: 'Insights', icon: BiData },
    { name: 'Snapshot', icon: BiScreenshot },
    { name: 'Asset Map', icon: BiShieldQuarter },
];

export default function Result() {
    const navigate = useNavigate();
    const [allData, setAllData] = useState([]);
    const [activeIndex, setActiveIndex] = useState(0);
    const data = allData[activeIndex] || null;
    const [activeTab, setActiveTab] = useState(0);
    const [dynamicColors, setDynamicColors] = useState({
        primary: '#4338ca',
        secondary: '#059669',
        accent: '#7c3aed'
    });

    useEffect(() => {
        const stored = localStorage.getItem('lastExtraction');
        if (stored) {
            let parsed = JSON.parse(stored);
            if (!Array.isArray(parsed)) {
                parsed = [parsed];
            }
            if (parsed.length > 0) {
                setAllData(parsed);
            } else {
                navigate('/');
            }
        } else {
            navigate('/');
        }
    }, [navigate]);

    useEffect(() => {
        if (data) {
            if (data.colorPalette && data.colorPalette.length >= 3) {
                setDynamicColors({
                    primary: data.colorPalette[0],
                    secondary: data.colorPalette[1],
                    accent: data.colorPalette[2] || data.colorPalette[0]
                });
            } else if (data.colorPalette && data.colorPalette.length > 0) {
                setDynamicColors({
                    primary: data.colorPalette[0],
                    secondary: data.colorPalette[0],
                    accent: data.colorPalette[0]
                });
            }
        }
    }, [data]);

    if (!data) return null;

    const visibleTabs = tabs.filter(t => t.name !== 'Snapshot' || data.screenshotBase64);
    const reportId = Math.floor(Math.random() * 90000) + 10000;

    return (
        <div className="min-h-screen pb-24 relative overflow-hidden bg-[#020617]">
            {/* Dynamic ambient glows */}
            <div
                className="rounded-full w-[800px] h-[800px] blur-[160px] absolute -top-[200px] -left-[200px] opacity-10 z-0 pointer-events-none transition-all duration-1000"
                style={{ backgroundColor: dynamicColors.primary }}
            />
            <div
                className="rounded-full w-[600px] h-[600px] blur-[140px] absolute top-[15%] -right-[150px] opacity-8 z-0 pointer-events-none transition-all duration-1000"
                style={{ backgroundColor: dynamicColors.secondary }}
            />
            <div
                className="rounded-full w-[700px] h-[700px] blur-[150px] absolute -bottom-[150px] left-[15%] opacity-6 z-0 pointer-events-none transition-all duration-1000"
                style={{ backgroundColor: dynamicColors.accent }}
            />

            <Navbar />

            <div className="max-w-[1280px] mx-auto pt-20 md:pt-24 px-4 md:px-8 relative z-10">
                <div className="flex flex-col gap-10 md:gap-16 animate-fade-in">

                    {/* Header */}
                    <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-8">
                        <div className="flex flex-col gap-4">
                            <div className="flex items-center gap-3 flex-wrap">
                                <span
                                    className="text-xs font-black px-3 py-1 rounded-full border bg-white/5 text-gray-200"
                                    style={{ borderColor: dynamicColors.primary + '88' }}
                                >
                                    REPORT ID: #{reportId}
                                </span>
                                <span className="text-gray-500 text-sm font-semibold truncate max-w-[400px]">
                                    {data.baseUrl}
                                </span>
                            </div>
                            <h1
                                className="text-4xl md:text-5xl font-black tracking-tight"
                                style={{
                                    background: `linear-gradient(to bottom right, #e2e8f0, ${dynamicColors.primary}, ${dynamicColors.secondary})`,
                                    WebkitBackgroundClip: 'text',
                                    WebkitTextFillColor: 'transparent',
                                    backgroundClip: 'text',
                                }}
                            >
                                {data.title || "Extraction Summary"}
                            </h1>
                        </div>

                        <div className="flex items-center gap-4 w-full lg:w-auto">
                            <button
                                onClick={() => navigate('/')}
                                className="flex items-center gap-2 h-12 px-8 rounded-2xl border border-white/10 text-gray-400 text-sm font-bold hover:bg-white/5 hover:text-gray-100 transition-all duration-300"
                                style={{ ['--hover-border']: dynamicColors.primary }}
                            >
                                <BiArrowBack className="w-4 h-4" /> START OVER
                            </button>
                            <DownloadFileComponent taskId={data.taskId} />
                        </div>
                    </div>

                    {/* URL Selector for Bulk results */}
                    {allData.length > 1 && (
                        <div className="flex bg-white/5 p-2 rounded-2xl gap-2 overflow-x-auto whitespace-nowrap mb-[-1rem]">
                            {allData.map((d, i) => (
                                <button
                                    key={i}
                                    onClick={() => setActiveIndex(i)}
                                    className={`px-4 py-2 rounded-xl text-sm font-bold transition-all ${
                                        activeIndex === i
                                            ? 'text-white shadow-lg bg-white/10'
                                            : 'text-gray-400 hover:text-white hover:bg-white/5'
                                    }`}
                                >
                                    {d.baseUrl || `Target ${i + 1}`}
                                </button>
                            ))}
                        </div>
                    )}

                    {/* Tabs */}
                    <div>
                        <div className="inline-flex gap-2 bg-white/2 p-2 rounded-3xl border border-white/5 mb-10 flex-wrap">
                            {visibleTabs.map((tab, idx) => {
                                const Icon = tab.icon;
                                const isActive = activeTab === idx;
                                return (
                                    <button
                                        key={tab.name}
                                        onClick={() => setActiveTab(idx)}
                                        className={`flex items-center gap-2 px-8 py-3.5 rounded-[18px] text-xs font-black tracking-[0.1em] uppercase transition-all duration-300 ${
                                            isActive ? 'text-gray-100' : 'text-gray-500 hover:text-gray-300'
                                        }`}
                                        style={isActive ? {
                                            background: dynamicColors.primary,
                                            boxShadow: `0 8px 25px ${dynamicColors.primary}40`
                                        } : {}}
                                    >
                                        <Icon className="w-4 h-4" />
                                        {tab.name}
                                    </button>
                                );
                            })}
                        </div>

                        {/* Tab Panels */}
                        <motion.div
                            key={activeTab}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: 0.4 }}
                        >
                            {/* Insights */}
                            {activeTab === 0 && (
                                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                                    <div className="lg:col-span-2">
                                        <SummaryCard summary={data.summary} />
                                    </div>
                                    <div>
                                        <TechStackCard tech={data.techStack} colors={data.colorPalette} />
                                    </div>
                                    <div className="lg:col-span-3">
                                        <SeoAuditCard issues={data.seoIssues} />
                                    </div>
                                </div>
                            )}

                            {/* Snapshot */}
                            {visibleTabs[activeTab]?.name === 'Snapshot' && data.screenshotBase64 && (
                                <div className="glass-effect rounded-[40px] overflow-hidden p-4 border border-indigo-400/10 bg-black/60">
                                    <img
                                        src={`data:image/png;base64,${data.screenshotBase64}`}
                                        alt="Visual Analysis"
                                        className="rounded-[32px] w-full max-h-[1000px] object-contain hover:scale-[1.01] transition-transform duration-500"
                                        loading="lazy"
                                    />
                                </div>
                            )}

                            {/* Asset Map */}
                            {visibleTabs[activeTab]?.name === 'Asset Map' && (
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                    <motion.div whileHover={{ y: -12 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
                                        <LinkCard taskId={data.taskId} />
                                    </motion.div>
                                    <motion.div whileHover={{ y: -12 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
                                        <ImageCard taskId={data.taskId} />
                                    </motion.div>
                                    <motion.div whileHover={{ y: -12 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
                                        <TextCard taskId={data.taskId} />
                                    </motion.div>
                                </div>
                            )}
                        </motion.div>
                    </div>

                </div>
            </div>
        </div>
    )
}

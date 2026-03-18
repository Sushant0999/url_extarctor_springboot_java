import React from 'react';
import { BiCheckCircle, BiErrorCircle, BiCheckShield } from 'react-icons/bi';

const statusConfig = {
    PASS:    { color: 'text-emerald-400', badge: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' },
    WARNING: { color: 'text-orange-400',  badge: 'bg-orange-500/20 text-orange-300 border-orange-500/30' },
    FAIL:    { color: 'text-pink-400',    badge: 'bg-pink-500/20 text-pink-300 border-pink-500/30' },
};

export default function SeoAuditCard({ issues }) {
    return (
        <div className="premium-card rounded-[40px] overflow-hidden bg-transparent">
            <div className="p-10 flex flex-col gap-10 w-full">
                {/* Header */}
                <div className="flex items-center gap-4">
                    <div className="bg-gradient-to-br from-pink-400 to-pink-700 p-3 rounded-[18px]">
                        <BiCheckShield className="text-white w-6 h-6" />
                    </div>
                    <div className="flex flex-col">
                        <h3 className="text-lg font-black text-gray-100 tracking-wide">SEO Integrity Scan</h3>
                        <span className="text-[10px] font-bold text-gray-500 tracking-[0.2em] uppercase">DIAGNOSTIC ANALYSIS</span>
                    </div>
                </div>

                {/* Grid of issues */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 w-full">
                    {issues && Array.isArray(issues) && issues.map((issue, idx) => {
                        const cfg = statusConfig[issue.status] || statusConfig.FAIL;
                        const Icon = issue.status === 'PASS' ? BiCheckCircle : BiErrorCircle;
                        return (
                            <div
                                key={idx}
                                className="flex items-start gap-4 p-6 bg-white/5 rounded-3xl border border-white/5 transition-all duration-300 hover:bg-white/8 hover:-translate-y-1"
                            >
                                <Icon className={`${cfg.color} w-6 h-6 flex-shrink-0 mt-0.5`} />
                                <div className="flex flex-col gap-1.5 min-w-0">
                                    <span className="text-gray-300 font-black text-xs tracking-[0.05em] uppercase">
                                        {issue.title?.toUpperCase()}
                                    </span>
                                    <div className="flex items-center gap-2 flex-wrap">
                                        <span className={`text-[9px] font-black tracking-widest uppercase px-2 py-0.5 rounded border ${cfg.badge}`}>
                                            {issue.status}
                                        </span>
                                        <span className="text-gray-500 text-[9px] truncate">{issue.description}</span>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                    {(!issues || issues.length === 0) && (
                        <p className="text-gray-500 italic text-sm">No SEO issues detected.</p>
                    )}
                </div>
            </div>
        </div>
    );
}

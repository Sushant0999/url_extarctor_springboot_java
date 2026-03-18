import React from 'react';
import { BiLayer, BiPalette } from 'react-icons/bi';

export default function TechStackCard({ tech, colors }) {
    const colorList = Array.isArray(colors) ? colors : (colors ? [colors] : []);

    return (
        <div className="premium-card rounded-[40px] h-full">
            <div className="p-10 flex flex-col gap-10 h-full">
                {/* Core Stack section */}
                <div className="w-full">
                    <div className="flex items-center gap-3 mb-5">
                        <div className="bg-emerald-500 p-2.5 rounded-[14px]">
                            <BiLayer className="text-white w-5 h-5" />
                        </div>
                        <h3 className="text-sm font-black text-gray-100 tracking-wide uppercase">CORE STACK</h3>
                    </div>
                    <div className="flex flex-wrap gap-3">
                        {tech && tech.map((t, idx) => (
                            <span
                                key={idx}
                                className="text-[10px] font-black text-emerald-300 tracking-wider uppercase px-4 py-1.5 rounded-xl border border-emerald-500/20 bg-white/5"
                            >
                                {t.toUpperCase()}
                            </span>
                        ))}
                        {(!tech || tech.length === 0) && (
                            <span className="text-gray-500 text-xs">No major frameworks detected.</span>
                        )}
                    </div>
                </div>

                {/* Visual DNA section */}
                <div className="w-full">
                    <div className="flex items-center gap-3 mb-5">
                        <div className="bg-indigo-500 p-2.5 rounded-[14px]">
                            <BiPalette className="text-white w-5 h-5" />
                        </div>
                        <h3 className="text-sm font-black text-gray-100 tracking-wide uppercase">VISUAL DNA</h3>
                    </div>
                    <div className="flex flex-wrap gap-6">
                        {colorList.map((color, idx) => (
                            <div key={idx} className="flex flex-col items-center gap-2">
                                <div
                                    className="w-10 h-10 rounded-full border-2 border-white/10 shadow-[0_8px_15px_rgba(0,0,0,0.3)]"
                                    style={{ backgroundColor: color }}
                                />
                                <span className="text-[9px] font-bold text-gray-500 tracking-[0.05em] uppercase">
                                    {typeof color === 'string' ? color.toUpperCase() : 'COLOR'}
                                </span>
                            </div>
                        ))}
                        {colorList.length === 0 && (
                            <span className="text-gray-500 text-xs">No color palette detected.</span>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

import React from 'react';
import { BiFile } from 'react-icons/bi';

export default function SummaryCard({ summary }) {
    return (
        <div className="premium-card rounded-[40px] h-full overflow-hidden">
            <div className="p-10 h-full flex flex-col gap-6">
                <div className="flex items-center justify-between w-full">
                    <div className="bg-gradient-to-br from-[#818cf8] to-[#6366f1] p-3.5 rounded-[22px]">
                        <BiFile className="text-white w-6 h-6" />
                    </div>
                    <span className="text-[10px] font-black tracking-[0.1em] uppercase bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 px-3 py-1 rounded-full">
                        EXECUTIVE SUMMARY
                    </span>
                </div>

                <div>
                    <h3 className="text-lg font-black text-gray-100 mb-3 tracking-wide">Page Context Analysis</h3>
                    <p className="text-gray-300 text-lg italic leading-[1.8] font-medium">
                        "{summary || "Synthesizing page content analysis..."}"
                    </p>
                </div>
            </div>
        </div>
    );
}

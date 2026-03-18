import React, { useState } from 'react';
import { getText } from '../../apis/GetText';
import { BiText, BiX } from 'react-icons/bi';

export default function TextCard({ taskId }) {
    const [isOpen, setIsOpen] = useState(false);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleOpen = () => { setIsOpen(true); fetchData(); };
    const handleClose = () => setIsOpen(false);

    const fetchData = async () => {
        try {
            setLoading(true);
            const content = await getText(taskId);
            setData(content);
        } catch (error) {
            console.error('Error fetching text:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            {/* Card */}
            <div className="glass-effect overflow-hidden rounded-3xl h-full border border-white/5 hover:border-teal-400/50 transition-all duration-300 flex flex-col">
                <div className="p-8 flex flex-col gap-4 flex-1">
                    <div className="bg-teal-600 p-3 rounded-xl w-fit">
                        <BiText className="text-white w-6 h-6" />
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-gray-100">Paragraphs</p>
                        <span className="text-[10px] font-black tracking-widest uppercase bg-teal-500/20 text-teal-300 px-2 py-0.5 rounded">
                            CONTENT
                        </span>
                    </div>
                    <p className="text-gray-400 text-sm">
                        Capture all text blocks, descriptions, and article content found on the page.
                    </p>
                </div>
                <div className="bg-white/2 border-t border-white/5 p-4">
                    <button onClick={handleOpen}
                        className="w-full text-teal-300 hover:text-teal-200 hover:bg-teal-500/10 py-2 rounded-xl text-sm font-bold transition-all duration-300">
                        Read Text
                    </button>
                </div>
            </div>

            {/* Modal */}
            {isOpen && (
                <div className="fixed inset-0 z-[300] flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={handleClose} />
                    <div className="relative z-10 glass-effect bg-[rgba(15,23,42,0.97)] rounded-[32px] border border-white/10 w-full max-w-3xl max-h-[80vh] flex flex-col shadow-2xl">
                        <div className="flex items-center justify-between px-8 py-6 border-b border-white/5">
                            <div className="flex items-center gap-3">
                                <BiText className="text-teal-400 w-5 h-5" />
                                <span className="text-gray-100 font-bold">Extracted Content</span>
                            </div>
                            <button onClick={handleClose} className="text-gray-400 hover:text-gray-100 transition-colors">
                                <BiX className="w-6 h-6" />
                            </button>
                        </div>
                        <div className="overflow-y-auto py-6 px-8 flex-1">
                            {loading ? (
                                <div className="flex flex-col gap-4">
                                    {[1,2,3,4].map(i => (
                                        <div key={i} className="h-20 bg-white/5 rounded-xl animate-pulse" />
                                    ))}
                                </div>
                            ) : data && data.length > 0 ? (
                                <div className="flex flex-col gap-6">
                                    {data.map((text, idx) => (
                                        <div key={idx}>
                                            <p className="text-base leading-relaxed text-gray-200">{text}</p>
                                            {idx < data.length - 1 && (
                                                <hr className="mt-6 border-white/10" />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="py-16 text-center">
                                    <p className="text-gray-400">No text content extracted from this page.</p>
                                </div>
                            )}
                        </div>
                        <div className="px-8 py-5 border-t border-white/5 flex justify-end">
                            <button onClick={handleClose}
                                className="bg-teal-600 hover:bg-teal-500 text-white px-6 py-2 rounded-full text-sm font-bold transition-all duration-300">
                                Dismiss
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

import React, { useState } from 'react';
import { getLink } from '../../apis/GetLinks';
import { BiLink, BiLinkExternal, BiX } from 'react-icons/bi';

export default function LinkCard({ taskId }) {
    const [isOpen, setIsOpen] = useState(false);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleOpen = () => { setIsOpen(true); fetchData(); };
    const handleClose = () => setIsOpen(false);

    const fetchData = async () => {
        try {
            setLoading(true);
            const links = await getLink(taskId);
            setData(links);
        } catch (error) {
            console.error('Error fetching links:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            {/* Card */}
            <div className="glass-effect overflow-hidden rounded-3xl h-full border border-white/5 hover:border-purple-400/50 transition-all duration-300 flex flex-col">
                <div className="p-8 flex flex-col gap-4 flex-1">
                    <div className="bg-purple-600 p-3 rounded-xl w-fit">
                        <BiLink className="text-white w-6 h-6" />
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-gray-100">Links</p>
                        <span className="text-[10px] font-black tracking-widest uppercase bg-purple-500/20 text-purple-300 px-2 py-0.5 rounded">
                            NAVIGATION
                        </span>
                    </div>
                    <p className="text-gray-400 text-sm">
                        Discover all internal and external hyperlinks found across the document.
                    </p>
                </div>
                <div className="bg-white/2 border-t border-white/5 p-4">
                    <button
                        onClick={handleOpen}
                        className="w-full text-purple-300 hover:text-purple-200 hover:bg-purple-500/10 py-2 rounded-xl text-sm font-bold transition-all duration-300"
                    >
                        Explore Links
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
                                <BiLink className="text-purple-400 w-5 h-5" />
                                <span className="text-gray-100 font-bold">Discovered Links</span>
                            </div>
                            <button onClick={handleClose} className="text-gray-400 hover:text-gray-100 transition-colors">
                                <BiX className="w-6 h-6" />
                            </button>
                        </div>
                        <div className="overflow-y-auto py-4 px-8 flex-1">
                            {loading ? (
                                <div className="flex flex-col gap-3 py-4">
                                    {[1,2,3,4,5].map(i => (
                                        <div key={i} className="h-12 bg-white/5 rounded-xl animate-pulse" />
                                    ))}
                                </div>
                            ) : data && data.length > 0 ? (
                                <div className="flex flex-col divide-y divide-white/5">
                                    {data.map((link, idx) => (
                                        <div key={idx} className="flex items-center justify-between py-3 hover:bg-white/3 rounded-lg px-2 transition-colors">
                                            <a href={link} target="_blank" rel="noreferrer"
                                                className="text-blue-300 text-sm truncate max-w-[500px] hover:text-blue-200 transition-colors">
                                                {link}
                                            </a>
                                            <BiLinkExternal className="text-gray-500 flex-shrink-0 mx-2 w-4 h-4" />
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="py-16 text-center">
                                    <p className="text-gray-400">No links discovered on this page.</p>
                                </div>
                            )}
                        </div>
                        <div className="px-8 py-5 border-t border-white/5 flex justify-end">
                            <button onClick={handleClose}
                                className="bg-purple-600 hover:bg-purple-500 text-white px-6 py-2 rounded-full text-sm font-bold transition-all duration-300">
                                Dismiss
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

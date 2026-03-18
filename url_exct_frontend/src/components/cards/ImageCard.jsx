import React, { useState } from 'react';
import { getImages } from '../../apis/GetImages';
import ImageDisplay from '../../utils/ImageDisplay';
import { BiImage, BiX } from 'react-icons/bi';

export default function ImageCard({ taskId }) {
    const [isOpen, setIsOpen] = useState(false);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    const handleOpen = () => { setIsOpen(true); fetchData(); };
    const handleClose = () => { setIsOpen(false); setErrorMsg(''); };

    const fetchData = async () => {
        try {
            setLoading(true);
            setErrorMsg('');
            const links = await getImages(taskId);
            setData(links);
        } catch (error) {
            console.error('Error fetching images:', error);
            if (error.response && error.response.status === 400) {
                setErrorMsg('No images were found in the last extraction. Try running the extraction again or use a different URL.');
            } else {
                setErrorMsg('Failed to fetch images. Please check your connection.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            {/* Card */}
            <div className="glass-effect overflow-hidden rounded-3xl h-full border border-white/5 hover:border-blue-400/50 transition-all duration-300 flex flex-col">
                <div className="p-8 flex flex-col gap-4 flex-1">
                    <div className="bg-blue-600 p-3 rounded-xl w-fit">
                        <BiImage className="text-white w-6 h-6" />
                    </div>
                    <div>
                        <p className="text-2xl font-bold text-gray-100">Images</p>
                        <span className="text-[10px] font-black tracking-widest uppercase bg-blue-500/20 text-blue-300 px-2 py-0.5 rounded">
                            STILL ASSETS
                        </span>
                    </div>
                    <p className="text-gray-400 text-sm">
                        Extract all images including inline graphics, logos, and high-res photos.
                    </p>
                </div>
                <div className="bg-white/2 border-t border-white/5 p-4">
                    <button onClick={handleOpen}
                        className="w-full text-blue-300 hover:text-blue-200 hover:bg-blue-500/10 py-2 rounded-xl text-sm font-bold transition-all duration-300">
                        View Gallery
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
                                <BiImage className="text-blue-400 w-5 h-5" />
                                <span className="text-gray-100 font-bold">Extracted Images</span>
                            </div>
                            <button onClick={handleClose} className="text-gray-400 hover:text-gray-100 transition-colors">
                                <BiX className="w-6 h-6" />
                            </button>
                        </div>
                        <div className="overflow-y-auto py-6 px-8 flex-1">
                            {loading ? (
                                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                                    {[1,2,3,4,5,6].map(i => (
                                        <div key={i} className="h-36 bg-white/5 rounded-2xl animate-pulse" />
                                    ))}
                                </div>
                            ) : errorMsg ? (
                                <div className="py-16 text-center flex flex-col items-center gap-4">
                                    <p className="text-gray-400 text-center">{errorMsg}</p>
                                    <button onClick={fetchData}
                                        className="text-xs font-bold text-blue-300 border border-blue-400/30 px-4 py-2 rounded-full hover:bg-blue-500/10 transition-colors">
                                        Retry
                                    </button>
                                </div>
                            ) : data && data.length > 0 ? (
                                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                                    {data.map((link, key) => (
                                        <div key={key} className="hover:scale-105 transition-transform duration-200">
                                            <ImageDisplay imageData={link} />
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="py-16 text-center">
                                    <p className="text-gray-400">No images available for this page.</p>
                                </div>
                            )}
                        </div>
                        <div className="px-8 py-5 border-t border-white/5 flex justify-end">
                            <button onClick={handleClose}
                                className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-2 rounded-full text-sm font-bold transition-all duration-300">
                                Dismiss
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

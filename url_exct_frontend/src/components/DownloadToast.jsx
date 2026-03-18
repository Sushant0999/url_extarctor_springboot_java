import { useState } from 'react';
import { getData } from "../apis/GetData";
import { BiDownload } from 'react-icons/bi';

const DownloadFileComponent = ({ taskId }) => {
    const [loading, setLoading] = useState(false);
    const [status, setStatus] = useState(null); // 'success' | 'error' | null

    const handleDownloadFile = async () => {
        setLoading(true);
        setStatus(null);
        try {
            await getData(taskId);
            setStatus('success');
        } catch (error) {
            console.error("Error downloading data:", error);
            setStatus('error');
        } finally {
            setLoading(false);
            setTimeout(() => setStatus(null), 3000);
        }
    };

    return (
        <button
            onClick={handleDownloadFile}
            disabled={loading}
            className={`flex items-center gap-2 h-12 px-6 rounded-2xl text-sm font-bold transition-all duration-300
                ${status === 'success' ? 'bg-emerald-600 text-white' :
                  status === 'error'   ? 'bg-red-600 text-white' :
                  'bg-gradient-to-r from-[#818cf8] to-[#6366f1] text-white shadow-[0_6px_20px_-6px_rgba(99,102,241,0.5)] hover:-translate-y-0.5 hover:shadow-[0_10px_30px_-6px_rgba(99,102,241,0.6)]'}
                ${loading ? 'opacity-60 cursor-not-allowed' : ''}
            `}
        >
            {loading ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
                <BiDownload className="w-4 h-4" />
            )}
            {status === 'success' ? 'Downloaded!' : status === 'error' ? 'Error!' : 'Download'}
        </button>
    );
};

export default DownloadFileComponent;

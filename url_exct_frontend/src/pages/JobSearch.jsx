import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { BiSearch, BiMap, BiBriefcase, BiCodeAlt, BiTimeFive, BiDollar, BiLinkExternal, BiFilterAlt, BiCloudUpload, BiCheckCircle } from 'react-icons/bi';
import Nav from '../components/Navbar';
import { searchJobs } from '../apis/Jobs';
import { parseResume } from '../apis/Resume';
import { getCountries, getStates, getCities } from '../apis/Location';
import * as XLSX from 'xlsx';
import { BiDownload, BiTrash, BiSelection, BiTargetLock } from 'react-icons/bi';
import { updateFavicon } from '../utils/favicon';

export default function JobSearch() {
    const [filters, setFilters] = useState(() => {
        const saved = localStorage.getItem('job_search_filters');
        return saved ? JSON.parse(saved) : {
            query: '',
            locations: [],
            jobType: '',
            workMode: '',
            experienceLevel: '',
            datePosted: '',
            country: 'IN',
            state: '',
            skills: [],
            additionalKeywords: '',
            platforms: ['indeed'],
            companies: [],
            maxPages: 1
        };
    });
    const [results, setResults] = useState(() => {
        const saved = localStorage.getItem('job_search_results');
        return saved ? JSON.parse(saved) : [];
    });
    
    // Auto-save to localStorage
    useEffect(() => {
        localStorage.setItem('job_search_filters', JSON.stringify(filters));
    }, [filters]);

    useEffect(() => {
        localStorage.setItem('job_search_results', JSON.stringify(results));
    }, [results]);

    const PLATFORM_DISPLAY_NAMES = {
        indeed: 'Indeed (Global)',
        linkedin: 'LinkedIn',
        naukri: 'Naukri.com',
        cutshort: 'Cutshort',
        foundit: 'Foundit.in',
        internshala: 'Internshala',
        shine: 'Shine.com',
        hirist: 'Hirist (Tech)'
    };

    const [countries, setCountries] = useState([]);
    const [states, setStates] = useState([]);
    const [fetchingCountries, setFetchingCountries] = useState(false);
    const [fetchingStates, setFetchingStates] = useState(false);
    const [showCountryDropdown, setShowCountryDropdown] = useState(false);
    const [showStateDropdown, setShowStateDropdown] = useState(false);
    const [showJobTypeDropdown, setShowJobTypeDropdown] = useState(false);
    const [showWorkModeDropdown, setShowWorkModeDropdown] = useState(false);
    const [showExpDropdown, setShowExpDropdown] = useState(false);
    const [showDateDropdown, setShowDateDropdown] = useState(false);
    const [showPlatformDropdown, setShowPlatformDropdown] = useState(false);
    const [platformStatus, setPlatformStatus] = useState({}); // { indeed: 'searching', linkedin: 'done' }
    const [searchingPlatform, setSearchingPlatform] = useState(null);
    const [sourceFilter, setSourceFilter] = useState('all');
    const [selectedJobLinks, setSelectedJobLinks] = useState(new Set());
    const [showExportModal, setShowExportModal] = useState(false);
    const [exportFormat, setExportFormat] = useState('xlsx'); // 'xlsx' or 'csv'
    const [exportFields, setExportFields] = useState({
        title: true,
        company: true,
        location: true,
        salary: true,
        link: true,
        source: true,
        datePosted: true
    });
    const [countrySearch, setCountrySearch] = useState('');
    const [stateSearchInput, setStateSearchInput] = useState('');
    const [companySearchInput, setCompanySearchInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Initial rounded favicon
    useEffect(() => {
        updateFavicon(false);
    }, []);

    // Sync favicon with search status
    useEffect(() => {
        updateFavicon(loading);
    }, [loading]);

    const addCompany = (companyName) => {
        if (companyName.trim() && !filters.companies.includes(companyName.trim())) {
            setFilters(prev => ({ ...prev, companies: [...prev.companies, companyName.trim()] }));
        }
        setCompanySearchInput('');
    };

    const removeCompany = (companyName) => {
        setFilters(prev => ({ ...prev, companies: prev.companies.filter(c => c !== companyName) }));
    };

    const togglePlatform = (p) => {
        setFilters(prev => ({
            ...prev,
            platforms: prev.platforms.includes(p) 
                ? prev.platforms.filter(plat => plat !== p)
                : [...prev.platforms, p]
        }));
    };

    const filteredCountries = countries.filter(c => 
        c.name.toLowerCase().includes(countrySearch.toLowerCase()) ||
        c.iso2.toLowerCase().includes(countrySearch.toLowerCase())
    );

    const filteredStates = states.filter(s => 
        s.name.toLowerCase().includes(stateSearchInput.toLowerCase()) ||
        s.iso2.toLowerCase().includes(stateSearchInput.toLowerCase())
    );

    // Close dropdowns on outside click
    useEffect(() => {
        const handleClickOutside = () => {
            setShowCountryDropdown(false);
            setShowStateDropdown(false);
            setShowJobTypeDropdown(false);
            setShowWorkModeDropdown(false);
            setShowExpDropdown(false);
            setShowDateDropdown(false);
        };
        document.addEventListener('click', handleClickOutside);
        return () => document.removeEventListener('click', handleClickOutside);
    }, []);

    // Initial fetch for countries
    useEffect(() => {
        const fetchCountries = async () => {
            setFetchingCountries(true);
            const data = await getCountries();
            setCountries(data);
            setFetchingCountries(false);
        };
        fetchCountries();
    }, []);

    // Fetch states whenever country changes
    useEffect(() => {
        const fetchStates = async () => {
            if (!filters.country) {
                setStates([]);
                return;
            }
            setFetchingStates(true);
            const data = await getStates(filters.country);
            setStates(data);
            setFetchingStates(false);
        };
        fetchStates();
    }, [filters.country]);

    const addState = (stateObj) => {
        if (!filters.locations.includes(stateObj.name)) {
            setFilters(prev => ({ 
                ...prev, 
                state: stateObj.iso2,
                locations: [...prev.locations, stateObj.name] 
            }));
        }
        setStateSearchInput('');
        setShowStateDropdown(false);
    };

    const removeLocation = (locToRemove) => {
        setFilters(prev => ({ ...prev, locations: prev.locations.filter(l => l !== locToRemove) }));
    };
    const [skillInput, setSkillInput] = useState('');
    const [parsingResume, setParsingResume] = useState(false);
    const [resumeParsedMsg, setResumeParsedMsg] = useState(null);

    const handleResumeUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setParsingResume(true);
        setError(null);
        setResumeParsedMsg(null);

        try {
            const data = await parseResume(file);
            console.log('Resume parsed:', data);
            setFilters(prev => ({
                ...prev,
                query: data.jobTitle || prev.query,
                skills: data.skills ? [...new Set([...prev.skills, ...data.skills])] : prev.skills,
                experienceLevel: data.experienceLevel || prev.experienceLevel,
                locations: data.location && !prev.locations.includes(data.location) 
                    ? [...prev.locations, data.location] 
                    : prev.locations
            }));
            setResumeParsedMsg("Resume parsed successfully! Filters updated.");
            setTimeout(() => setResumeParsedMsg(null), 5000);
        } catch (err) {
            console.error('Error parsing resume:', err);
            setError("Failed to parse resume. Please ensure it's a valid PDF or DOCX.");
        } finally {
            setParsingResume(false);
        }
    };

    const performSearch = async () => {
        setLoading(true);
        setError(null);
        setResults([]);
        setSourceFilter('all');
        setSelectedJobLinks(new Set());
        
        const platformsToSearch = filters.platforms.length > 0 ? filters.platforms : ['indeed'];
        const numPages = filters.maxPages || 1;
        const tasks = [];
        const initialStatus = {};

        // Generate tasks: platform + page
        platformsToSearch.forEach(p => {
            for (let page = 1; page <= numPages; page++) {
                const taskId = `${p}_page_${page}`;
                tasks.push({ platform: p, page: page, id: taskId });
                initialStatus[taskId] = 'pending';
            }
        });
        
        setPlatformStatus(initialStatus);

        // Helper for Parallel Search with Retry and Timeout
        const searchWithRetry = async (platform, page, taskId, attempt = 1) => {
            const MAX_RETRIES = 3;
            const TIMEOUT_MS = 45000;
            
            setPlatformStatus(prev => ({ ...prev, [taskId]: attempt > 1 ? `retrying (${attempt}/3)` : 'searching' }));
            
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

            try {
                const platformFilters = { ...filters, platforms: [platform], page: page };
                const data = await searchJobs(platformFilters, { signal: controller.signal });
                clearTimeout(timeoutId);

                // Update results incrementally (Streaming effect)
                setResults(prev => {
                    const next = [...prev, ...data];
                    // Client-side dedup just in case
                    return next.filter((v, i, a) => a.findIndex(t => t.link === v.link) === i);
                });

                setPlatformStatus(prev => ({ 
                    ...prev, 
                    [taskId]: 'done', 
                    [`${taskId}_count`]: data.length 
                }));
                return data;
            } catch (err) {
                clearTimeout(timeoutId);
                const isTimeout = err.name === 'AbortError' || err.message?.toLowerCase().includes('timeout');
                
                if (isTimeout) {
                    setPlatformStatus(prev => ({ ...prev, [taskId]: 'timeout' }));
                    return [];
                }

                if (attempt < MAX_RETRIES) {
                    // Randomized backoff for retries
                    await new Promise(r => setTimeout(r, 1000 * attempt + Math.random() * 1000));
                    return searchWithRetry(platform, page, taskId, attempt + 1);
                }

                setPlatformStatus(prev => ({ ...prev, [taskId]: 'failed' }));
                return [];
            }
        };

        try {
            // Adaptive Concurrency: Limit to 4 simultaneous browsers to prevent hardware lag
            const CONCURRENCY_LIMIT = 4;
            const queue = [...tasks];
            const activeWorkers = [];

            while (queue.length > 0 || activeWorkers.length > 0) {
                // Fill up worker slots
                while (queue.length > 0 && activeWorkers.length < CONCURRENCY_LIMIT) {
                    const task = queue.shift();
                    const worker = (async () => {
                        try {
                            await searchWithRetry(task.platform, task.page, task.id);
                        } finally {
                            // Remove itself from active workers
                            activeWorkers.splice(activeWorkers.indexOf(worker), 1);
                        }
                    })();
                    activeWorkers.push(worker);
                }
                
                // Wait for any worker to finish before adding more
                if (activeWorkers.length > 0) {
                    await Promise.race(activeWorkers);
                }
            }
            
            setResults(prev => {
                if (prev.length === 0) {
                    setError("No jobs found with these filters across selected platforms.");
                }
                return prev;
            });
        } catch (err) {
            console.error("Critical parallel search error:", err);
            setError("An unexpected error occurred during search.");
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = (e) => {
        if (e) e.preventDefault();
        performSearch();
    };

    const toggleJobSelection = (link) => {
        setSelectedJobLinks(prev => {
            const next = new Set(prev);
            if (next.has(link)) next.delete(link);
            else next.add(link);
            return next;
        });
    };

    const selectAllJobs = () => {
        const filteredResults = results.filter(job => sourceFilter === 'all' || job.source === sourceFilter);
        const allLinks = new Set(filteredResults.map(j => j.link));
        setSelectedJobLinks(allLinks);
    };

    const clearSelection = () => setSelectedJobLinks(new Set());

    const handleExport = () => {
        const selectedData = results.filter(job => selectedJobLinks.has(job.link));
        
        // Prepare data based on selected fields
        const exportData = selectedData.map(job => {
            const row = {};
            if (exportFields.title) row['Job Title'] = job.title;
            if (exportFields.company) row['Company'] = job.company;
            if (exportFields.location) row['Location'] = job.location;
            if (exportFields.salary) row['Salary'] = job.salary;
            
            if (exportFields.link) {
                if (exportFormat === 'xlsx') {
                    // Excel hyperlink formula
                    row['Link'] = { t: 's', v: job.link, f: `HYPERLINK("${job.link}", "View Job")` };
                } else {
                    row['Link'] = job.link;
                }
            }
            
            if (exportFields.source) row['Source'] = job.source;
            if (exportFields.datePosted) row['Date Posted'] = job.datePosted;
            return row;
        });

        const wb = XLSX.utils.book_new();
        const ws = XLSX.utils.json_to_sheet(exportData);
        XLSX.utils.book_append_sheet(wb, ws, "Jobs");

        if (exportFormat === 'xlsx') {
            XLSX.writeFile(wb, `Jobs_Export_${new Date().toLocaleDateString()}.xlsx`);
        } else {
            XLSX.writeFile(wb, `Jobs_Export_${new Date().toLocaleDateString()}.csv`, { bookType: 'csv' });
        }
        setShowExportModal(false);
    };

    const addSkill = () => {
        if (skillInput.trim()) {
            if (filters.skills.length >= 7) {
                setError("Maximum 7 skills allowed to maintain search accuracy");
                return;
            }
            if (!filters.skills.includes(skillInput.trim())) {
                setFilters(prev => ({ ...prev, skills: [...prev.skills, skillInput.trim()] }));
            }
            setSkillInput('');
            setError(null);
        }
    };

    const removeSkill = (skillToRemove) => {
        setFilters(prev => ({ ...prev, skills: prev.skills.filter(s => s !== skillToRemove) }));
    };

    return (
        <div className="min-h-screen bg-[#030712] relative overflow-hidden">
            {/* Background elements */}
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-900/10 blur-[120px] rounded-full pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-emerald-900/10 blur-[120px] rounded-full pointer-events-none" />

            <Nav isSearching={loading} />

            <div className="max-w-[1200px] mx-auto pt-24 pb-20 px-4 relative z-10">
                <motion.div 
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-center mb-16"
                >
                    <h1 className="text-5xl md:text-6xl font-[800] tracking-tight mb-4 text-gradient">
                        Find Your Next <span className="bg-gradient-to-r from-indigo-400 to-emerald-400 bg-clip-text text-transparent">Opportunity</span>
                    </h1>
                    <p className="text-gray-400 max-w-2xl mx-auto text-lg">
                        Our AI-powered scraper scours top job boards based on your specific skills and preferences.
                    </p>
                </motion.div>

                {/* Resume Upload Section */}
                <motion.div 
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="mb-8 flex flex-col items-center"
                >
                    <div className="glass-effect rounded-[24px] p-4 flex items-center gap-6 w-full max-w-xl border-dashed border-2 border-white/10 hover:border-indigo-500/30 transition-all group overflow-hidden relative">
                        {parsingResume && (
                            <div className="absolute inset-0 bg-indigo-600/10 backdrop-blur-sm flex items-center justify-center z-20">
                                <div className="flex items-center gap-3">
                                    <div className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                                    <span className="text-xs font-[700] uppercase tracking-widest text-white">Analyzing Resume...</span>
                                </div>
                            </div>
                        )}
                        <div className="bg-indigo-500/10 p-4 rounded-2xl group-hover:scale-110 transition-transform">
                            <BiCloudUpload className="text-indigo-400 w-8 h-8" />
                        </div>
                        <div className="flex-1">
                            <h3 className="text-sm font-[700] text-gray-200 mb-1">Boost search with your Resume</h3>
                            <p className="text-[10px] text-gray-500 uppercase tracking-wider font-[600]">Upload PDF or DOCX to auto-fill filters</p>
                        </div>
                        <label className="bg-white/10 hover:bg-white/20 px-6 py-2.5 rounded-xl text-xs font-[800] uppercase tracking-widest cursor-pointer transition-all">
                            Browse
                            <input type="file" accept=".pdf,.docx" className="hidden" onChange={handleResumeUpload} />
                        </label>
                    </div>

                    <AnimatePresence>
                        {resumeParsedMsg && (
                            <motion.div 
                                initial={{ opacity: 0, y: -10 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0 }}
                                className="mt-4 flex items-center gap-2 text-emerald-400 text-xs font-[700] uppercase tracking-widest"
                            >
                                <BiCheckCircle className="w-4 h-4" />
                                {resumeParsedMsg}
                            </motion.div>
                        )}
                    </AnimatePresence>
                </motion.div>

                {/* Filter Section */}
                <motion.div 
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="glass-effect rounded-[32px] p-6 md:p-8 mb-12"
                >
                    <form onSubmit={handleSearch} className="space-y-6">
                        {/* Search Progress Dashboard */}
                        {(loading || Object.keys(platformStatus).length > 0) && (
                            <div className="mb-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                                 {Object.entries(platformStatus)
                                    .filter(([key]) => !key.endsWith('_count')) // Don't loop over the count keys
                                    .filter(([_, status]) => status !== 'pending')
                                    .map(([plat, status]) => (
                                    <div key={plat} className="bg-white/5 border border-white/10 rounded-2xl p-4 flex items-center justify-between">
                                        <div className="flex flex-col">
                                            <span className="text-[10px] font-[800] text-gray-500 uppercase tracking-widest mb-1">
                                                {PLATFORM_DISPLAY_NAMES[plat.split('_page_')[0]] || plat} 
                                                {plat.includes('_page_') && ` - Page ${plat.split('_page_')[1]}`}
                                            </span>
                                            <span className="text-xs font-[700] text-white">Status: {status.charAt(0).toUpperCase() + status.slice(1)}</span>
                                        </div>
                                        <div className="flex flex-col items-end">
                                            {status === 'done' && (
                                                <span className="text-[10px] font-[800] text-emerald-400 bg-emerald-400/10 px-2 py-0.5 rounded-full border border-emerald-400/20">
                                                    {platformStatus[`${plat}_count`] || 0} Results
                                                </span>
                                            )}
                                            {status === 'searching' && (
                                                <div className="flex items-center gap-2">
                                                    <div className="w-1.5 h-1.5 bg-indigo-500 rounded-full animate-ping" />
                                                    <span className="text-[10px] font-[800] text-indigo-400 uppercase">Searching...</span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                            {/* Country Selector */}
                            <div className="space-y-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Target Country</label>
                                <div className="relative group">
                                    <BiMap className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-indigo-400 transition-colors pointer-events-none" />
                                    <div 
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white cursor-pointer hover:border-white/20 transition-all flex justify-between items-center"
                                        onClick={() => setShowCountryDropdown(!showCountryDropdown)}
                                    >
                                        <span className={filters.country ? 'text-white' : 'text-gray-500'}>
                                            {countries.find(c => c.iso2 === filters.country)?.name || "Select Country"}
                                        </span>
                                        <div className={`w-2 h-2 border-r-2 border-b-2 border-gray-500 transform transition-transform ${showCountryDropdown ? '-rotate-135' : 'rotate-45'}`}></div>
                                    </div>

                                    <AnimatePresence>
                                        {showCountryDropdown && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+8px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                <div className="p-2 border-b border-white/5 bg-white/5">
                                                    <input 
                                                        type="text"
                                                        placeholder="Search country..."
                                                        className="w-full bg-transparent text-sm text-white focus:outline-none p-2"
                                                        value={countrySearch}
                                                        onChange={(e) => setCountrySearch(e.target.value)}
                                                        onClick={(e) => e.stopPropagation()}
                                                        autoFocus
                                                    />
                                                </div>
                                                <div className="max-h-[250px] overflow-y-auto custom-scrollbar">
                                                    {filteredCountries.map(c => (
                                                        <div 
                                                            key={c.iso2}
                                                            className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-gray-300 hover:text-white transition-colors flex items-center justify-between"
                                                            onClick={() => {
                                                                setFilters(prev => ({ ...prev, country: c.iso2, state: '', locations: [] }));
                                                                setShowCountryDropdown(false);
                                                                setCountrySearch('');
                                                            }}
                                                        >
                                                            {c.name}
                                                            <span className="text-[10px] text-gray-500 font-mono italic">{c.iso2}</span>
                                                        </div>
                                                    ))}
                                                    {filteredCountries.length === 0 && (
                                                        <div className="px-4 py-8 text-center text-xs text-gray-500 italic">No countries found</div>
                                                    )}
                                                </div>
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* Platform Selector */}
                            <div className="space-y-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Target Platforms</label>
                                <div className="relative group">
                                    <BiCodeAlt className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-indigo-400 transition-colors pointer-events-none" />
                                    <div 
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white cursor-pointer hover:border-white/20 transition-all flex justify-between items-center"
                                        onClick={() => setShowPlatformDropdown(!showPlatformDropdown)}
                                    >
                                        <span className={filters.platforms.length > 0 ? 'text-white' : 'text-gray-500'}>
                                            {filters.platforms.length === 0 ? "Any Platform" : (filters.platforms.length === 1 ? filters.platforms[0].toUpperCase() : `${filters.platforms.length} Platforms`)}
                                        </span>
                                        <div className={`w-2 h-2 border-r-2 border-b-2 border-gray-500 transform transition-transform ${showPlatformDropdown ? '-rotate-135' : 'rotate-45'}`}></div>
                                    </div>

                                    <AnimatePresence>
                                        {showPlatformDropdown && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+4px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                {[
                                                    { id: 'indeed', name: 'Indeed (Global)' },
                                                    { id: 'linkedin', name: 'LinkedIn' },
                                                    { id: 'naukri', name: 'Naukri.com' },
                                                    { id: 'cutshort', name: 'Cutshort' },
                                                    { id: 'foundit', name: 'Foundit.in' },
                                                    { id: 'internshala', name: 'Internshala' },
                                                    { id: 'shine', name: 'Shine.com' },
                                                    { id: 'hirist', name: 'Hirist (Tech)' }
                                                ].map(plat => (
                                                    <div 
                                                        key={plat.id}
                                                        className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm transition-colors flex items-center justify-between"
                                                        onClick={() => togglePlatform(plat.id)}
                                                    >
                                                        <span className={filters.platforms.includes(plat.id) ? 'text-white font-[700]' : 'text-gray-400'}>{plat.name}</span>
                                                        {filters.platforms.includes(plat.id) && <BiCheckCircle className="text-emerald-400" />}
                                                    </div>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* State Selection */}
                            <div className="space-y-3 md:col-span-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Target States / Provinces</label>
                                
                                {/* Selected States Chips */}
                                <div className="flex flex-wrap gap-2 mb-3 min-h-[32px]">
                                    <AnimatePresence>
                                        {filters.locations.map(loc => (
                                            <motion.span 
                                                key={loc}
                                                initial={{ opacity: 0, scale: 0.8 }}
                                                animate={{ opacity: 1, scale: 1 }}
                                                exit={{ opacity: 0, scale: 0.8, x: -10 }}
                                                className="bg-indigo-500/10 text-indigo-300 px-4 py-1.5 rounded-xl text-xs font-[700] flex items-center gap-3 border border-indigo-500/20 hover:border-indigo-500/40 transition-all cursor-default"
                                            >
                                                {loc}
                                                <button 
                                                    type="button" 
                                                    onClick={() => removeLocation(loc)}
                                                    className="hover:bg-red-500/20 p-1 rounded-md transition-colors"
                                                >
                                                    <BiFilterAlt className="w-3 h-3 rotate-45" />
                                                </button>
                                            </motion.span>
                                        ))}
                                    </AnimatePresence>
                                </div>

                                {/* Custom State Input/Dropdown */}
                                <div className="relative group">
                                    <BiMap className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-emerald-400 transition-colors pointer-events-none" />
                                    <input 
                                        type="text"
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white focus:outline-none focus:border-emerald-500/50 appearance-none transition-all placeholder:text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed"
                                        placeholder={fetchingStates ? "Caching states..." : (filters.country ? "Search states..." : "Select a country first")}
                                        value={stateSearchInput}
                                        disabled={!filters.country || fetchingStates}
                                        onFocus={() => setShowStateDropdown(true)}
                                        onChange={(e) => {
                                            setStateSearchInput(e.target.value);
                                            setShowStateDropdown(true);
                                        }}
                                        onKeyPress={(e) => {
                                            if (e.key === 'Enter') {
                                                e.preventDefault();
                                                if (filteredStates.length > 0) addState(filteredStates[0]);
                                            }
                                        }}
                                    />

                                    <AnimatePresence>
                                        {showStateDropdown && filters.country && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+8px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                <div className="max-h-[250px] overflow-y-auto custom-scrollbar">
                                                    {filteredStates.map(s => (
                                                        <div 
                                                            key={s.iso2}
                                                            className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-gray-300 hover:text-white transition-colors flex items-center justify-between"
                                                            onClick={() => addState(s)}
                                                        >
                                                            {s.name}
                                                            <span className="text-[10px] text-gray-500 font-mono italic">{s.iso2}</span>
                                                        </div>
                                                    ))}
                                                    {filteredStates.length === 0 && (
                                                        <div className="px-4 py-8 text-center text-xs text-gray-500 italic">No states found</div>
                                                    )}
                                                </div>
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>
                        </div>

                        {/* Secondary Filter Grid */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 pt-8 border-t border-white/5">
                            {/* Query */}
                            <div className="space-y-2 lg:col-span-2">
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Job Title / Role</label>
                                <div className="relative group">
                                    <BiSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-indigo-400 transition-colors" />
                                    <input 
                                        type="text" 
                                        placeholder="e.g. Software Engineer"
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white focus:outline-none focus:border-indigo-500/50 transition-all font-[500]"
                                        value={filters.query}
                                        onChange={(e) => setFilters(prev => ({ ...prev, query: e.target.value }))}
                                        required
                                    />
                                </div>
                            </div>

                            {/* Job Type */}
                            <div className="space-y-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Job Type</label>
                                <div className="relative group">
                                    <BiBriefcase className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-indigo-400 transition-colors pointer-events-none" />
                                    <div 
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white cursor-pointer hover:border-white/20 transition-all flex justify-between items-center"
                                        onClick={() => setShowJobTypeDropdown(!showJobTypeDropdown)}
                                    >
                                        <span className={filters.jobType ? 'text-white' : 'text-gray-500'}>
                                            {filters.jobType ? filters.jobType.charAt(0).toUpperCase() + filters.jobType.slice(1) : "Any Type"}
                                        </span>
                                        <div className={`w-2 h-2 border-r-2 border-b-2 border-gray-500 transform transition-transform ${showJobTypeDropdown ? '-rotate-135' : 'rotate-45'}`}></div>
                                    </div>

                                    <AnimatePresence>
                                        {showJobTypeDropdown && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+4px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                {[
                                                    { value: '', label: 'Any Type' },
                                                    { value: 'fulltime', label: 'Full-time' },
                                                    { value: 'parttime', label: 'Part-time' },
                                                    { value: 'contract', label: 'Contract' },
                                                    { value: 'internship', label: 'Internship' }
                                                ].map(opt => (
                                                    <div 
                                                        key={opt.value}
                                                        className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-gray-300 hover:text-white transition-colors"
                                                        onClick={() => {
                                                            setFilters(prev => ({ ...prev, jobType: opt.value }));
                                                            setShowJobTypeDropdown(false);
                                                        }}
                                                    >
                                                        {opt.label}
                                                    </div>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* Work Mode */}
                            <div className="space-y-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Work Mode</label>
                                <div className="relative group">
                                    <BiBriefcase className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-emerald-400 transition-colors pointer-events-none" />
                                    <div 
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white cursor-pointer hover:border-white/20 transition-all flex justify-between items-center"
                                        onClick={() => setShowWorkModeDropdown(!showWorkModeDropdown)}
                                    >
                                        <span className={filters.workMode ? 'text-white' : 'text-gray-500'}>
                                            {filters.workMode === 'onsite' ? 'On-site' : filters.workMode === 'remote' ? 'Remote' : filters.workMode === 'hybrid' ? 'Hybrid' : "Any Mode"}
                                        </span>
                                        <div className={`w-2 h-2 border-r-2 border-b-2 border-gray-500 transform transition-transform ${showWorkModeDropdown ? '-rotate-135' : 'rotate-45'}`}></div>
                                    </div>

                                    <AnimatePresence>
                                        {showWorkModeDropdown && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+4px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                {[
                                                    { value: '', label: 'Any Mode' },
                                                    { value: 'onsite', label: 'On-site' },
                                                    { value: 'remote', label: 'Remote' },
                                                    { value: 'hybrid', label: 'Hybrid' }
                                                ].map(opt => (
                                                    <div 
                                                        key={opt.value}
                                                        className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-gray-300 hover:text-white transition-colors"
                                                        onClick={() => {
                                                            setFilters(prev => ({ ...prev, workMode: opt.value }));
                                                            setShowWorkModeDropdown(false);
                                                        }}
                                                    >
                                                        {opt.label}
                                                    </div>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* Experience Level */}
                            <div className="space-y-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Experience Level</label>
                                <div className="relative group">
                                    <BiBriefcase className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-emerald-400 transition-colors pointer-events-none" />
                                    <div 
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white cursor-pointer hover:border-white/20 transition-all flex justify-between items-center"
                                        onClick={() => setShowExpDropdown(!showExpDropdown)}
                                    >
                                        <span className={filters.experienceLevel ? 'text-white' : 'text-gray-500'}>
                                            {filters.experienceLevel === 'entry_level' ? 'Entry Level' : filters.experienceLevel === 'mid_level' ? 'Mid Level' : filters.experienceLevel === 'senior_level' ? 'Senior Level' : "All Levels"}
                                        </span>
                                        <div className={`w-2 h-2 border-r-2 border-b-2 border-gray-500 transform transition-transform ${showExpDropdown ? '-rotate-135' : 'rotate-45'}`}></div>
                                    </div>

                                    <AnimatePresence>
                                        {showExpDropdown && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+4px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                {[
                                                    { value: '', label: 'All Levels' },
                                                    { value: 'entry_level', label: 'Entry Level' },
                                                    { value: 'mid_level', label: 'Mid Level' },
                                                    { value: 'senior_level', label: 'Senior Level' }
                                                ].map(opt => (
                                                    <div 
                                                        key={opt.value}
                                                        className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-gray-300 hover:text-white transition-colors"
                                                        onClick={() => {
                                                            setFilters(prev => ({ ...prev, experienceLevel: opt.value }));
                                                            setShowExpDropdown(false);
                                                        }}
                                                    >
                                                        {opt.label}
                                                    </div>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* Date Posted */}
                            <div className="space-y-2 relative" onClick={(e) => e.stopPropagation()}>
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Date Posted</label>
                                <div className="relative group">
                                    <BiTimeFive className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-indigo-400 transition-colors pointer-events-none" />
                                    <div 
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-white cursor-pointer hover:border-white/20 transition-all flex justify-between items-center"
                                        onClick={() => setShowDateDropdown(!showDateDropdown)}
                                    >
                                        <span className={filters.datePosted ? 'text-white' : 'text-gray-500'}>
                                            {filters.datePosted === '1' ? 'Last 24 hours' : filters.datePosted === '3' ? 'Last 3 days' : filters.datePosted === '7' ? 'Last 7 days' : filters.datePosted === '14' ? 'Last 14 days' : "Anytime"}
                                        </span>
                                        <div className={`w-2 h-2 border-r-2 border-b-2 border-gray-500 transform transition-transform ${showDateDropdown ? '-rotate-135' : 'rotate-45'}`}></div>
                                    </div>

                                    <AnimatePresence>
                                        {showDateDropdown && (
                                            <motion.div 
                                                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                                                className="absolute top-[calc(100%+4px)] left-0 right-0 bg-[#111827] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-[100] backdrop-blur-xl"
                                            >
                                                {[
                                                    { value: '', label: 'Anytime' },
                                                    { value: '1', label: 'Last 24 hours' },
                                                    { value: '3', label: 'Last 3 days' },
                                                    { value: '7', label: 'Last 7 days' },
                                                    { value: '14', label: 'Last 14 days' }
                                                ].map(opt => (
                                                    <div 
                                                        key={opt.value}
                                                        className="px-4 py-3 hover:bg-white/5 cursor-pointer text-sm text-gray-300 hover:text-white transition-colors"
                                                        onClick={() => {
                                                            setFilters(prev => ({ ...prev, datePosted: opt.value }));
                                                            setShowDateDropdown(false);
                                                        }}
                                                    >
                                                        {opt.label}
                                                    </div>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* Additional Keywords */}
                            <div className="space-y-2 lg:col-span-2">
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Additional Keywords</label>
                                <div className="relative group">
                                    <BiFilterAlt className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-emerald-400 transition-colors" />
                                    <input 
                                        type="text" 
                                        placeholder="e.g. Startup, Fintech, AI"
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-sm text-white focus:outline-none focus:border-emerald-500/50 transition-all"
                                        value={filters.additionalKeywords}
                                        onChange={(e) => setFilters(prev => ({ ...prev, additionalKeywords: e.target.value }))}
                                    />
                                </div>
                            </div>

                            {/* Target Companies */}
                            <div className="space-y-2 lg:col-span-2 relative">
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Target Companies (Leave empty for all)</label>
                                
                                {/* Selected Company Chips */}
                                <div className="flex flex-wrap gap-2 mb-2 min-h-[32px]">
                                    <AnimatePresence>
                                        {filters.companies.map(comp => (
                                            <motion.span 
                                                key={comp}
                                                initial={{ opacity: 0, scale: 0.8 }}
                                                animate={{ opacity: 1, scale: 1 }}
                                                exit={{ opacity: 0, scale: 0.8, x: -10 }}
                                                className="bg-emerald-500/10 text-emerald-300 px-3 py-1 rounded-xl text-[11px] font-[600] flex items-center gap-2 border border-emerald-500/20"
                                            >
                                                {comp}
                                                <button type="button" onClick={() => removeCompany(comp)} className="hover:text-white transition-colors">
                                                    <BiFilterAlt className="w-3 h-3 rotate-45" />
                                                </button>
                                            </motion.span>
                                        ))}
                                    </AnimatePresence>
                                </div>

                                <div className="relative group">
                                    <BiBriefcase className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-emerald-400 transition-colors pointer-events-none" />
                                    <input 
                                        type="text" 
                                        placeholder="e.g. Google, TCS, Microsoft"
                                        className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-11 pr-4 text-sm text-white focus:outline-none focus:border-emerald-500/50 transition-all"
                                        value={companySearchInput}
                                        onChange={(e) => setCompanySearchInput(e.target.value)}
                                        onKeyPress={(e) => {
                                            if (e.key === 'Enter') {
                                                e.preventDefault();
                                                addCompany(companySearchInput);
                                            }
                                        }}
                                    />
                                    <button 
                                        type="button"
                                        onClick={() => addCompany(companySearchInput)}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 bg-white/5 hover:bg-white/10 px-3 py-1.5 rounded-xl text-[10px] font-bold text-gray-400"
                                    >
                                        ADD
                                    </button>
                                </div>
                            </div>
                        </div>

                        {/* Skills Section */}
                        <div className="pt-8 border-t border-white/5">
                            <div className="space-y-3">
                                <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 ml-1">Required Skills</label>
                                <div className="flex flex-wrap gap-2 mb-3">
                                    <AnimatePresence>
                                        {filters.skills.map(skill => (
                                            <motion.span 
                                                key={skill}
                                                initial={{ opacity: 0, scale: 0.8 }}
                                                animate={{ opacity: 1, scale: 1 }}
                                                exit={{ opacity: 0, scale: 0.8 }}
                                                className="bg-indigo-500/20 text-indigo-300 px-3 py-1 rounded-full text-xs font-[600] flex items-center gap-2 border border-indigo-500/30"
                                            >
                                                {skill}
                                                <button type="button" onClick={() => removeSkill(skill)} className="hover:text-white">×</button>
                                            </motion.span>
                                        ))}
                                    </AnimatePresence>
                                </div>
                                <div className="flex gap-2">
                                    <div className="relative flex-1 group">
                                        <BiCodeAlt className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-indigo-400 transition-colors" />
                                        <input 
                                            type="text" 
                                            placeholder="Add skill (e.g. React, Docker)"
                                            className="w-full bg-white/5 border border-white/10 rounded-2xl py-3 pl-11 pr-4 text-sm text-white focus:outline-none focus:border-indigo-500/50 transition-all"
                                            value={skillInput}
                                            onChange={(e) => setSkillInput(e.target.value)}
                                            onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addSkill())}
                                        />
                                    </div>
                                    <button 
                                        type="button" 
                                        onClick={addSkill}
                                        className="bg-white/10 hover:bg-white/20 px-6 rounded-2xl text-xs font-[700] uppercase tracking-wider transition-all"
                                    >
                                        Add
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div className="flex justify-center pt-6">
                            <motion.button 
                                whileHover={{ scale: 1.05 }}
                                whileTap={{ scale: 0.95 }}
                                type="submit"
                                disabled={loading}
                                className={`
                                    px-12 py-4 rounded-2xl font-[800] tracking-[0.1em] uppercase text-sm
                                    shadow-[0_20px_40px_-15px_rgba(99,102,241,0.4)]
                                    ${loading ? 'bg-gray-700 cursor-not-allowed' : 'bg-gradient-to-r from-indigo-600 to-indigo-500 text-white'}
                                    transition-all duration-300
                                `}
                            >
                                {loading ? 'Scraping Jobs...' : 'Search Opportunities'}
                            </motion.button>
                        </div>
                    </form>
                </motion.div>

                {/* Status/Error Messages */}
                {error && (
                    <motion.div 
                        initial={{ opacity: 0 }} 
                        animate={{ opacity: 1 }}
                        className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-center mb-8"
                    >
                        {error}
                    </motion.div>
                )}

                {/* Source Filter Chips */}
                {results.length > 0 && (
                    <div className="flex flex-wrap items-center gap-3 mb-8 pb-4 border-b border-white/5 overflow-x-auto no-scrollbar">
                        <span className="text-[10px] font-[700] uppercase tracking-[0.2em] text-gray-500 mr-2">Filter by Source:</span>
                        {[...new Set(['all', ...results.map(j => j.source)])].map(source => (
                            <button
                                key={source}
                                onClick={() => setSourceFilter(source)}
                                className={`
                                    px-4 py-1.5 rounded-full text-[11px] font-[700] uppercase tracking-wider transition-all
                                    ${sourceFilter === source 
                                        ? 'bg-indigo-500 text-white shadow-[0_0_15px_rgba(99,102,241,0.4)]' 
                                        : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-gray-300'}
                                `}
                            >
                                {source === 'all' ? 'All Platforms' : source}
                            </button>
                        ))}
                    </div>
                )}

                {/* Selection Action Bar */}
                {results.length > 0 && (
                    <div className="flex items-center justify-between mb-6 px-4 py-3 bg-white/5 border border-white/10 rounded-2xl backdrop-blur-xl">
                        <div className="flex items-center gap-4">
                            <button 
                                onClick={selectAllJobs}
                                className="text-[11px] font-[700] uppercase tracking-wider text-indigo-400 hover:text-indigo-300 transition-colors flex items-center gap-1.5"
                            >
                                <BiSelection className="w-4 h-4" />
                                Select All
                            </button>
                            <button 
                                onClick={clearSelection}
                                className="text-[11px] font-[700] uppercase tracking-wider text-gray-500 hover:text-rose-400 transition-colors flex items-center gap-1.5"
                            >
                                <BiTrash className="w-4 h-4" />
                                Clear
                            </button>
                        </div>
                        <div className="flex items-center gap-4">
                            <span className="text-xs font-[600] text-gray-400">
                                {selectedJobLinks.size} jobs selected
                            </span>
                            <button 
                                onClick={() => setShowExportModal(true)}
                                disabled={selectedJobLinks.size === 0}
                                className={`
                                    px-6 py-2 rounded-xl text-xs font-[700] uppercase tracking-wider transition-all flex items-center gap-2
                                    ${selectedJobLinks.size > 0 
                                        ? 'bg-emerald-500 text-white hover:bg-emerald-600 shadow-[0_10px_20px_-5px_rgba(16,185,129,0.4)]' 
                                        : 'bg-gray-700 text-gray-500 cursor-not-allowed'}
                                `}
                            >
                                <BiDownload className="w-4 h-4" />
                                Export
                            </button>
                        </div>
                    </div>
                )}

                {/* Results Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {loading && results.length === 0 ? (
                        // Shimmer loaders - only show if no results yet
                        [...Array(6)].map((_, i) => (
                            <div key={i} className="premium-card h-[320px] animate-pulse bg-white/[0.03] rounded-[32px] border border-white/5" />
                        ))
                    ) : (
                        results
                            .filter(job => sourceFilter === 'all' || job.source === sourceFilter)
                            .map((job, index) => (
                            <motion.div 
                                key={index}
                                initial={{ opacity: 0, y: 20 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: index * 0.05 }}
                                className={`
                                    premium-card p-6 flex flex-col justify-between group h-full relative overflow-hidden transition-all duration-300
                                    ${selectedJobLinks.has(job.link) ? 'ring-2 ring-indigo-500/50 bg-indigo-500/5' : ''}
                                `}
                                onClick={() => toggleJobSelection(job.link)}
                            >
                                {/* Selection Checkbox */}
                                <div className="absolute top-4 right-4 z-10">
                                    <div className={`
                                        w-5 h-5 rounded-md border-2 transition-all flex items-center justify-center
                                        ${selectedJobLinks.has(job.link) 
                                            ? 'bg-indigo-500 border-indigo-500' 
                                            : 'border-white/20 bg-white/5 opacity-0 group-hover:opacity-100'}
                                    `}>
                                        {selectedJobLinks.has(job.link) && <BiCheckCircle className="text-white w-4 h-4" />}
                                    </div>
                                </div>

                                <div>
                                    <div className="flex justify-between items-start mb-4">
                                        <div className="bg-indigo-500/10 p-2 rounded-xl border border-indigo-500/20">
                                            <BiBriefcase className="text-indigo-400 w-5 h-5" />
                                        </div>
                                        <span className="text-[10px] font-[800] uppercase tracking-widest text-[#10b981] bg-[#10b981]/10 px-3 py-1 rounded-full border border-[#10b981]/20">
                                            {job.source}
                                        </span>
                                    </div>
                                    
                                    <h3 className="text-lg font-[700] text-white mb-2 line-clamp-2 group-hover:text-indigo-400 transition-colors">
                                        {job.title}
                                    </h3>
                                    
                                    <div className="flex items-center gap-2 text-gray-400 text-sm mb-4">
                                        <span className="font-[600] text-gray-300">{job.company}</span>
                                        <span>•</span>
                                        <div className="flex items-center gap-1">
                                            <BiMap className="w-3.5 h-3.5" />
                                            {job.location}
                                        </div>
                                    </div>

                                    <div className="space-y-3 mb-6">
                                        <div className="flex items-center gap-3 text-xs text-gray-500">
                                            <BiDollar className="text-emerald-500 w-4 h-4" />
                                            {job.salary || 'Salary not disclosed'}
                                        </div>
                                        <div className="flex items-center gap-3 text-xs text-gray-500">
                                            <BiTimeFive className="text-indigo-500 w-4 h-4" />
                                            {job.datePosted || 'Posted recently'}
                                        </div>
                                    </div>
                                </div>

                                <a 
                                    href={job.link} 
                                    target="_blank" 
                                    rel="noopener noreferrer"
                                    className="w-full py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-center text-sm font-[700] flex items-center justify-center gap-2 transition-all"
                                >
                                    View details <BiLinkExternal />
                                </a>
                            </motion.div>
                        ))
                    )}
                </div>
            </div>

            {/* Export Customization Modal */}
            <AnimatePresence>
                {showExportModal && (
                    <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
                        onClick={() => setShowExportModal(false)}
                    >
                        <motion.div 
                            initial={{ scale: 0.9, y: 20 }}
                            animate={{ scale: 1, y: 0 }}
                            exit={{ scale: 0.9, y: 20 }}
                            className="bg-[#111827] border border-white/10 rounded-[32px] p-8 w-full max-w-md shadow-2xl"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <div className="flex justify-between items-center mb-6">
                                <h2 className="text-xl font-[800] text-white">Export Jobs</h2>
                                <button onClick={() => setShowExportModal(false)} className="text-gray-500 hover:text-white transition-colors text-2xl">×</button>
                            </div>

                            <div className="space-y-6">
                                {/* Format Selection */}
                                <div>
                                    <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 block mb-3">File Format</label>
                                    <div className="flex gap-3">
                                        {['xlsx', 'csv'].map(format => (
                                            <button
                                                key={format}
                                                onClick={() => setExportFormat(format)}
                                                className={`
                                                    flex-1 py-3 rounded-xl border transition-all text-sm font-[700] uppercase tracking-widest
                                                    ${exportFormat === format 
                                                        ? 'bg-indigo-500/20 border-indigo-500 text-indigo-400' 
                                                        : 'bg-white/5 border-white/10 text-gray-500 hover:border-white/20'}
                                                `}
                                            >
                                                {format}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* Field Selection */}
                                <div>
                                    <label className="text-xs font-[700] uppercase tracking-wider text-gray-400 block mb-3">Include Fields</label>
                                    <div className="grid grid-cols-2 gap-3">
                                        {Object.entries(exportFields).map(([key, value]) => (
                                            <button
                                                key={key}
                                                onClick={() => setExportFields(prev => ({ ...prev, [key]: !value }))}
                                                className={`
                                                    flex items-center gap-3 p-3 rounded-xl border transition-all text-left
                                                    ${value 
                                                        ? 'bg-white/10 border-indigo-500/50 text-white' 
                                                        : 'bg-white/[0.02] border-white/5 text-gray-600'}
                                                `}
                                            >
                                                <div className={`w-4 h-4 rounded border transition-all flex items-center justify-center ${value ? 'bg-indigo-500 border-indigo-500' : 'border-white/20'}`}>
                                                    {value && <BiCheckCircle className="text-white w-3 h-3" />}
                                                </div>
                                                <span className="text-xs font-[600] capitalize">{key.replace(/([A-Z])/g, ' $1')}</span>
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* Summary & Action */}
                                <div className="pt-4 border-t border-white/5 mt-8">
                                    <div className="flex justify-between items-center mb-6">
                                        <span className="text-sm text-gray-400">Total Selected:</span>
                                        <span className="text-sm font-[800] text-indigo-400">{selectedJobLinks.size} Jobs</span>
                                    </div>
                                    <button 
                                        onClick={handleExport}
                                        className="w-full py-4 rounded-2xl bg-gradient-to-r from-indigo-600 to-indigo-500 text-white font-[800] uppercase tracking-widest text-sm shadow-[0_20px_40px_-15px_rgba(99,102,241,0.4)] hover:scale-[1.02] transition-all"
                                    >
                                        Download {exportFormat.toUpperCase()}
                                    </button>
                                </div>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

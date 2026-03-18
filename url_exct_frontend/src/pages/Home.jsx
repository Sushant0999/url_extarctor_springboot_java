import React from 'react'
import Navbar from '../components/Navbar'
import SearchBox from '../components/SearchBox'

export default function Home() {
    return (
        <div className="min-h-screen relative overflow-hidden bg-[#030712]">
            {/* Ambient Background Glows */}
            <div className="rounded-full w-[800px] h-[800px] bg-blue-900 blur-[160px] absolute -top-[300px] -right-[200px] opacity-30 z-0 pointer-events-none" />
            <div className="rounded-full w-[600px] h-[600px] bg-purple-900 blur-[140px] absolute -bottom-[200px] -left-[100px] opacity-15 z-0 pointer-events-none" />

            <div className="relative z-10">
                <Navbar />
                <div className="max-w-[900px] mx-auto pt-20 md:pt-32 px-4">
                    <div className="flex flex-col gap-12 animate-fade-in">
                        <SearchBox />
                    </div>
                </div>
            </div>
        </div>
    )
}

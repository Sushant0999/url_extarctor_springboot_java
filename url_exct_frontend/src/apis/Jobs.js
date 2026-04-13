import axios from 'axios';

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
const API_BASE_URL = `${baseUrl}/api/jobs`;

export const searchJobs = async (filters, options = {}) => {
    try {
        // If a single platform is provided uniquely, intelligently map to its custom endpoint (e.g. /search/indeed)
        const platformPath = filters.platforms && filters.platforms.length === 1 
            ? `/${filters.platforms[0]}` 
            : '';
            
        const response = await axios.post(`${API_BASE_URL}/search${platformPath}`, filters, options);
        return response.data;
    } catch (error) {
        console.error('Error searching jobs:', error);
        throw error;
    }
};

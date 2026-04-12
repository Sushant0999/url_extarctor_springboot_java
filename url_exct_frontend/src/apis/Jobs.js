import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/jobs';

export const searchJobs = async (filters) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/search`, filters);
        return response.data;
    } catch (error) {
        console.error('Error searching jobs:', error);
        throw error;
    }
};

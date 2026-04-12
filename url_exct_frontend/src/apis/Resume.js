import axios from 'axios';

const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
const API_BASE_URL = `${baseUrl}/api/resume`;

export const parseResume = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    
    try {
        const response = await axios.post(`${API_BASE_URL}/parse`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data;
    } catch (error) {
        console.error('Error parsing resume:', error);
        throw error;
    }
};

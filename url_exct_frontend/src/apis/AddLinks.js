import axios from 'axios';

export const addLinks = async (link) => {
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
    const url = `${baseUrl}/urlData/extract`;

    try {
        // The backend expects a single URL string OR an array of URLs
        const response = await axios.post(url, link, {
            headers: {
                'Content-Type': 'application/json',
            }
        });
        return response.data; // This is the taskId mapping map
    } catch (error) {
        console.error('Error starting extraction task:', error);
        throw error;
    }
};

import axios from 'axios';

export const getImages = async (taskId) => {
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
    const url = `${baseUrl}/urlData/media/${taskId}`;

    try {
        const response = await axios.get(url, {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json',
            }
        });
        const data = response.data;
        return data;
    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
};

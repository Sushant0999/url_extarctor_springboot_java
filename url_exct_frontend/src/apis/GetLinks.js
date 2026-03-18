import axios from 'axios';

export const getLink = async (taskId) => {

    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
    const url = `${baseUrl}/urlData/getTags/${taskId}`;


    try {
        const response = await axios.get(url, {
            headers: {
                'Content-Type': 'application/json',
            },
            mode: 'no-cors'
        });
        const data = response.data;
        return data;
    } catch (error) {
        console.error('Error fetching tags:', error);
        throw error;
    }
};

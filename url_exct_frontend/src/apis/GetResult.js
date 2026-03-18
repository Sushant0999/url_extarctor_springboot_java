import axios from 'axios';

export const getResult = async (taskId) => {
    const baseUrl = import.meta.env.VITE_BASE_URL;
    const url = `${baseUrl}/urlData/result/${taskId}`;

    try {
        const response = await axios.get(url, {
            headers: {
                'Content-Type': 'application/json',
            }
        });
        return response.data; // Expected: ExtractedData object
    } catch (error) {
        console.error('Error fetching task results:', error);
        throw error;
    }
};

export const getBulkResults = async (taskIds) => {
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
    const url = `${baseUrl}/urlData/results/bulk`;

    try {
        const response = await axios.post(url, taskIds, {
            headers: { 'Content-Type': 'application/json' }
        });
        return response.data; // { taskId: ExtractedData, ... }
    } catch (error) {
        console.error('Error fetching bulk results:', error);
        throw error;
    }
};

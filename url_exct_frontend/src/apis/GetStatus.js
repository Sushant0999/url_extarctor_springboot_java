import axios from 'axios';

export const getStatus = async (taskId) => {
    const baseUrl = import.meta.env.VITE_BASE_URL;
    const url = `${baseUrl}/urlData/status/${taskId}`;

    try {
        const response = await axios.get(url, {
            headers: {
                'Content-Type': 'application/json',
            }
        });
        return response.data; // Expected: PENDING, IN_PROGRESS, COMPLETED, FAILED
    } catch (error) {
        console.error('Error fetching task status:', error);
        throw error;
    }
};

export const getBulkStatus = async (taskIds) => {
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
    const url = `${baseUrl}/urlData/status/bulk`;

    try {
        const response = await axios.post(url, taskIds, {
            headers: { 'Content-Type': 'application/json' }
        });
        return response.data; // { taskId: "COMPLETED", ... }
    } catch (error) {
        console.error('Error fetching bulk status:', error);
        throw error;
    }
};

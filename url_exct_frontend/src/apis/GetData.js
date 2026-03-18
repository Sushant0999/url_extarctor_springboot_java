import axios from 'axios';

export const getData = async (taskId) => {
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:8080';
    const url = `${baseUrl}/urlData/download/${taskId}`;

    try {
        const response = await axios.get(url, {
            responseType: 'blob',
        });

        const blob = new Blob([response.data], { type: response.headers['content-type'] });

        const tempLink = document.createElement('a');
        tempLink.href = window.URL.createObjectURL(blob);

        tempLink.setAttribute('download', 'data.zip');

        tempLink.click();

        window.URL.revokeObjectURL(tempLink.href);

        return response;

    } catch (error) {
        console.error('Error downloading file:', error);
        throw error;
    }
};

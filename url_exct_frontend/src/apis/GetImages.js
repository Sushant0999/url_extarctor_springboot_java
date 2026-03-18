import axios from 'axios';

export const getImages = async () => {
    const baseUrl = import.meta.env.VITE_BASE_URL;
    const url = `${baseUrl}/urlData/media`;

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

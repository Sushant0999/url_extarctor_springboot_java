import axios from 'axios';

export const getImages = async () => {
    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/getImages`;

    try {
        const response = await axios.get(url);
        const data = response.data;
        return data;
    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
};

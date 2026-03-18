import axios from 'axios';

export const getLink = async () => {

    const baseUrl = import.meta.env.VITE_BASE_URL;
    const url = `${baseUrl}/urlData/getTags`;


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

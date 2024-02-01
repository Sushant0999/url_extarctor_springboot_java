import axios from 'axios';

export const getImages = async () => {
    const url = 'https://jsonplaceholder.typicode.com/photos';

    try {
        const response = await axios.get(url);
        const data = response.data;

        return data;
    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
};

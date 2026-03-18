import axios from 'axios';

export const addLinks = async (link, enable) => {

    const baseUrl = import.meta.env.VITE_BASE_URL;
    const url = `${baseUrl}/urlData/extract`;

    const list = []
    list.push(link)

    let response;

    try {
        response = await axios.post(url, list, {
            headers: {
                'Content-Type': 'application/json',
            },
            mode: 'cors',
            date: list
        });
        // return response;

    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
    finally {
        return response;
    }
};

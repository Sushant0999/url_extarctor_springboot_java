import axios from 'axios';

export const getText = async () => {

    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/getText`;


    try {
        const response = await axios.get(url, {
            headers: {
                'Content-Type': 'application/json',
            },
            mode: 'no-cors'
        });
        const data = response.data;
        console.log(data);
        return data;
    } catch (error) {
        console.error('Error fetching tags:', error);
        throw error;
    }
};

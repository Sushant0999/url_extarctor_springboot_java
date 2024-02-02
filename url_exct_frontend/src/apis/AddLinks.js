import axios from 'axios';

export const addLinks = async (link, enable) => {

    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/insert`;

    console.log(url);

    const list = []
    list.push(link)

    try {
        const response = await axios.post(url, list, {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json',

            },
            params: {
                'jsEnable': enable
            }
        });
        return response;

    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
};

import axios from 'axios';

export const addLinks = async (link, enable) => {

    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/insert`;
    const list = []
    list.push(link)

    let response;

    try {
        response = await axios.post(url, list, {
            headers: {
                'Content-Type': 'application/json',
            },
            params: {
                'jsEnable': enable
            }
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

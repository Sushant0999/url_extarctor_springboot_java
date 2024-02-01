import axios from 'axios';

export const addLinks = async (link) => {

    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/insert`;

    console.log("baseUrl", baseUrl);
    console.log("url", url);

    const list = []
    list.push(link)


    try {
        const response = await axios.post(url, list, {
            headers: {
                // 'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json',
                // mode: 'cors'
            },
        });
        const data = response.data;

        console.log(data);
        return data;
    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
};

import axios from 'axios';

export const getData = async () => {
    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/getData`;

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

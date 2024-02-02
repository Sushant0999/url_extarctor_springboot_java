import axios from 'axios';
import ToastNotification from '../utils/ToastNotification';

export const addLinks = async (link) => {


    const baseUrl = process.env.REACT_APP_BASE_URL;
    const url = `${baseUrl}/urlData/insert`;

    // console.log("baseUrl", baseUrl);
    // console.log("url", url);

    const list = []
    list.push(link)


    try {
        const response = await axios.post(url, list, {
            headers: {
                'Content-Type': 'application/json',
            },
        });
        const data = response.data;

        switch (response.status) {
            case 200:
                <ToastNotification title={'Worked'} />
                break;
            case 204:
                <ToastNotification title={'No Content'} />
                break;
            case 500:
                <ToastNotification title={'Server Error'} />
                break;
            default:
                <ToastNotification title={'Something Went Wrong'} />
                break;
        }

        return data;
    } catch (error) {
        console.error('Error fetching images:', error);
        throw error;
    }
};

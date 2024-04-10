import React, { useEffect, useState } from 'react';

const ImageDisplay = ({ imageData }) => {
    const [source, setSource] = useState(null);

    useEffect(() => {
        const fetchImage = () => {
            try {
                if (!imageData) {
                    return;
                }
                const blob = dataURItoBlob(imageData);
                const dataUrl = URL.createObjectURL(blob);
                setSource(dataUrl);
            } catch (error) {
                console.error('Error fetching image:', error);
            }
        };

        fetchImage();
    }, [imageData]);

    const dataURItoBlob = (dataURI) => {
        const byteString = atob(dataURI);
        const ab = new ArrayBuffer(byteString.length);
        const ia = new Uint8Array(ab);
        for (let i = 0; i < byteString.length; i++) {
            ia[i] = byteString.charCodeAt(i);
        }
        return new Blob([ab], { type: 'image/png' });
    };

    return (
        <img
            src={source || 'placeholder_image_url.png'}
            // height={'70px'}
            style={{ border: '1px solid black', borderRadius: '10px' }}
            alt='Fetched Image'
        />
    );
};

export default ImageDisplay;
import { Box, Button, Input, useToast } from '@chakra-ui/react';
import React, { useState } from 'react';
import { addLinks } from '../apis/AddLinks';
import { ScatterBoxLoaderComponent } from './loaders/ScatterBoxLoaderComponent';

export default function SearchBox() {
    const inputPlaceholder = 'Enter Url';
    const [inputText, setInputText] = useState('');
    const [isLoading, setLoading] = useState(false);


    const toast = useToast()
    const toastIdRef = React.useRef()

    const handleInputChange = (event) => {
        setInputText(event.target.value);
    };

    function validateLinks(urls) {
        if (typeof urls === 'string') {
            const urlRegex = /^(ftp|http|https):\/\/[^ "]+$/;
            return urlRegex.test(urls);
        } else if (Array.isArray(urls)) {
            const urlRegex = /^(ftp|http|https):\/\/[^ "]+$/;
            return urls.some(url => urlRegex.test(url));
        } else {
            console.error('Invalid input type');
            return false;
        }
    }

    const handleSearch = async () => {
        if (inputText) {
            setLoading(true);
            await addLinks(inputText);
        }
        else {
            setTimeout(() => {
                setLoading(false);
            }, 1000);
        }
        setTimeout(() => {
            setLoading(false);
        }, 1000);
    };

    function addToast() {
        toastIdRef.current = toast({ description: 'Input Empty', isClosable: true })
    }

    return (
        <>
            {isLoading ?
                <Box sx={{ margin: '10% 0', display: 'flex', justifyContent: 'center', justifyItems: 'center' }}><ScatterBoxLoaderComponent /></Box> :
                <Box
                    gap={4}
                    sx={{
                        margin: '15% 0',
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        alignItems: 'center',
                        textAlign: 'center',
                    }}
                >
                    <Input
                        placeholder={inputPlaceholder}
                        value={inputText}
                        onChange={handleInputChange}
                        sx={{
                            display: 'block',
                            margin: '0 auto',
                            fontSize: '20px',
                            textAlign: 'center'
                        }}
                        width="80%"
                        fontSize="lg"
                        textAlign="center"
                    />
                    <Button colorScheme='teal' fontSize={'15px'} variant='outline' onClick={inputText ? handleSearch : addToast} >
                        Search
                    </Button>
                </Box>
            }
        </>
    );
}

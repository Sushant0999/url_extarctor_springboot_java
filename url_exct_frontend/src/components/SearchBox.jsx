import { Box, Button, Input, Switch, useToast } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import { addLinks } from '../apis/AddLinks';
import { ScatterBoxLoaderComponent } from './loaders/ScatterBoxLoaderComponent';
import { useNavigate } from 'react-router-dom';
import { Text } from '@chakra-ui/react'

export default function SearchBox() {

    const navigate = useNavigate();


    const inputPlaceholder = 'Enter Url';
    const [inputText, setInputText] = useState('');
    const [isLoading, setLoading] = useState(false);
    const [enable, isEnable] = useState(true);

    useEffect(() => {
        isEnable(true);
    }, []);

    const toast = useToast()
    const toastIdRef = React.useRef()

    const handleInputChange = (event) => {
        setInputText(event.target.value);
    };

    // function validateLinks(urls) {
    //     if (typeof urls === 'string') {
    //         const urlRegex = /^(ftp|http|https):\/\/[^ "]+$/;
    //         return urlRegex.test(urls);
    //     } else if (Array.isArray(urls)) {
    //         const urlRegex = /^(ftp|http|https):\/\/[^ "]+$/;
    //         return urls.some(url => urlRegex.test(url));
    //     } else {
    //         console.error('Invalid input type');
    //         return false;
    //     }
    // }

    const handleSearch = async () => {
        let message = '';
        let status = '';
        function createToast() {
            toastIdRef.current = toast({ description: message, status: status, isClosable: true })
        }
        if (inputText) {
            setLoading(true);
            let response;
            try {
                response = await addLinks(inputText, enable);
                console.log('from searchbox ', response === undefined);
                if (response.status === 200) {
                    navigate('/result');
                    message = 'Success';
                    status = 'success';
                    createToast();
                } else if (response.status === 204) {
                    message = 'Invalid Url or Resource Down Try Enable/Disable JavaScript';
                    status = 'info';
                    createToast();
                }
            } catch (e) {
                if (response === undefined) {
                    navigate('/')
                    message = 'Something Went Wrong';
                    status = 'error';
                    createToast();
                }
            }

            // switch (response.status) {
            //     case 200:
            //         navigate('/result');
            //         message = 'Success';
            //         status = 'success';
            //         createToast();
            //         break;
            //     case 204:
            //         message = 'Invalid Url or Resource Down Try Enable/Disable JavaScript';
            //         status = 'info';
            //         createToast();
            //         break;
            //     case undefined:
            //         message = 'Server Error';
            //         status = 'error';
            //         createToast();
            //         break;
            //     default:
            //         message = 'Try Again After Sometimes';
            //         status = 'info';
            //         createToast();
            //         break;
            // }
        }
        else {
            setTimeout(() => {
                setLoading(false);
            }, 1000);
            message = 'Error'
            status = 'error';
            createToast();
        }
        setTimeout(() => {
            setLoading(false);
        }, 1000);
    };

    function addToast() {
        toastIdRef.current = toast({ description: 'Input Empty', status: 'warning', isClosable: true })
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
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '80%', textAlign: 'center', textJustify: 'center' }}>
                        <Text fontSize={'20px'}>Enable Javascripts</Text>
                        <Switch sx={{ pt: 1.5 }} isChecked={enable} onChange={(e) => isEnable(e.target.checked)} colorScheme='teal' size='lg' />
                    </Box>
                    <Button colorScheme='teal' fontSize={'15px'} variant='outline' onClick={inputText ? handleSearch : addToast} >
                        Search
                    </Button>
                </Box>
            }
        </>
    );
}

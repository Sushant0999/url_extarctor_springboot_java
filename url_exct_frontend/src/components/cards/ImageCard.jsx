import React, { useState } from 'react';
import { Button, Card, CardBody, CardFooter, Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalFooter, Text, ModalBody, Box, Stack, Skeleton, SimpleGrid, Image } from '@chakra-ui/react';
import { getImages } from '../../apis/GetImages';
import ImageDisplay from '../../utils/ImageDisplay';
import image from '../../images/image.png'



export default function ImageCard() {
    const [isOpen, setIsOpen] = useState(false);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleOpen = () => {
        setIsOpen(true);
    };

    const handleClose = () => {
        setIsOpen(false);
    };

    const fetchData = async () => {
        try {
            setLoading(true);
            const links = await getImages();
            setData(links);
        } catch (error) {
            console.error('Error fetching links:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box>
            <Card sx={{
                display: 'flex',
                textAlign: 'center',
                // backgroundImage: `url(${image})`,
                backgroundSize: 'cover',
                backgroundRepeat: 'no-repeat',
                // filter: 'blur(2px)',
            }}>
                <CardBody >
                    <Text fontSize={'30px'}>Images</Text>
                </CardBody>
                <CardFooter sx={{ display: 'flex', justifyContent: 'center' }}>
                    <Button size='sm' onClick={() => { handleOpen(); fetchData(); }}>View here</Button>
                </CardFooter>
            </Card>
            <Modal isOpen={isOpen} onClose={handleClose} size={'xl'}>
                <ModalOverlay />
                <ModalContent>
                    <ModalHeader sx={{ textAlign: 'center' }}>Modal Title</ModalHeader>
                    <ModalCloseButton />
                    <ModalBody>
                        <Box sx={{ maxH: '400px', overflowY: 'scroll' }}>
                            {loading ? (
                                <Stack>
                                    <Skeleton height='200px' />
                                    <Skeleton height='200px' />
                                </Stack>
                            ) : (
                                <SimpleGrid >
                                    {data && data.length > 0 ? (
                                        <>
                                            {data.map((link, key) => (
                                                <ImageDisplay imageData={link} key={key} />
                                            ))}
                                        </>
                                    ) : (
                                        <></>
                                    )}
                                </SimpleGrid>
                            )}
                        </Box>
                    </ModalBody>
                    <ModalFooter>
                        <Button sx={{ display: 'flex', justifyContent: 'flex-start' }} colorScheme='blue' mr={3} onClick={handleClose}>
                            Close
                        </Button>
                        {/* <Button variant='ghost'>Secondary Action</Button> */}
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </Box >
    );
}

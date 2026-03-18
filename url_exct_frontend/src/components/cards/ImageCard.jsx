import React, { useState } from 'react';
import { Button, Card, CardBody, CardFooter, Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalFooter, Text, ModalBody, Box, Stack, Skeleton, SimpleGrid, Icon, VStack, Badge, HStack } from '@chakra-ui/react';
import { getImages } from '../../apis/GetImages';
import ImageDisplay from '../../utils/ImageDisplay';
import { BiImage } from 'react-icons/bi';

export default function ImageCard() {
    const [isOpen, setIsOpen] = useState(false);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    const handleOpen = () => {
        setIsOpen(true);
        fetchData();
    };

    const handleClose = () => {
        setIsOpen(false);
        setErrorMsg('');
    };

    const fetchData = async () => {
        try {
            setLoading(true);
            setErrorMsg('');
            const links = await getImages();
            setData(links);
        } catch (error) {
            console.error('Error fetching links:', error);
            if (error.response && error.response.status === 400) {
                setErrorMsg('No images were found in the last extraction. Try running the extraction again or use a different URL.');
            } else {
                setErrorMsg('Failed to fetch images. Please check your connection.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box>
            <Card 
                className="glass-effect"
                overflow="hidden"
                borderRadius="2xl"
                height="100%"
                border="1px solid rgba(255,255,255,0.05)"
                _hover={{ borderColor: 'blue.400' }}
                transition="all 0.3s"
            >
                <CardBody p={8}>
                    <VStack align="start" spacing={4}>
                        <Box bg="blue.500" p={3} borderRadius="xl">
                            <Icon as={BiImage} color="white" boxSize={6} />
                        </Box>
                        <Box>
                            <VStack align="start" spacing={0}>
                                <Text fontSize="2xl" fontWeight="bold" color="white">Images</Text>
                                <Badge colorScheme="blue" borderRadius="md" variant="subtle">STILL ASSETS</Badge>
                            </VStack>
                        </Box>
                        <Text color="gray.400" fontSize="sm">
                            Extract all images including inline graphics, logos, and high-res photos.
                        </Text>
                    </VStack>
                </CardBody>
                <CardFooter bg="rgba(255,255,255,0.02)" borderTop="1px solid rgba(255,255,255,0.05)">
                    <Button 
                        width="100%" 
                        colorScheme="blue" 
                        variant="ghost"
                        onClick={handleOpen}
                    >
                        View Gallery
                    </Button>
                </CardFooter>
            </Card>

            <Modal isOpen={isOpen} onClose={handleClose} size="3xl" scrollBehavior="inside">
                <ModalOverlay backdropFilter="blur(10px)" bg="blackAlpha.700" />
                <ModalContent 
                    className="glass-effect" 
                    bg="rgba(15, 23, 42, 0.95)" 
                    borderRadius="3xl"
                    border="1px solid rgba(255,255,255,0.1)"
                    color="white"
                >
                    <ModalHeader borderBottom="1px solid rgba(255,255,255,0.05)">
                        <HStack>
                            <Icon as={BiImage} color="blue.400" />
                            <Text>Extracted Images</Text>
                        </HStack>
                    </ModalHeader>
                    <ModalCloseButton />
                    <ModalBody py={6}>
                        {loading ? (
                            <SimpleGrid columns={[1, 2, 3]} spacing={4}>
                                {[1, 2, 3, 4, 5, 6].map(i => (
                                    <Skeleton key={i} height="150px" borderRadius="xl" />
                                ))}
                            </SimpleGrid>
                        ) : errorMsg ? (
                            <VStack p={10} spacing={4}>
                                <Text color="gray.400" textAlign="center">{errorMsg}</Text>
                                <Button size="sm" colorScheme="blue" variant="outline" onClick={fetchData}>Retry</Button>
                            </VStack>
                        ) : data && data.length > 0 ? (
                            <SimpleGrid columns={[1, 2, 3]} spacing={4}>
                                {data.map((link, key) => (
                                    <Box 
                                        key={key} 
                                        transition="transform 0.2s" 
                                        _hover={{ transform: 'scale(1.05)' }}
                                    >
                                        <ImageDisplay imageData={link} />
                                    </Box>
                                ))}
                            </SimpleGrid>
                        ) : (
                            <VStack p={10}>
                                <Text color="gray.400">No images available for this page.</Text>
                            </VStack>
                        )}
                    </ModalBody>
                    <ModalFooter borderTop="1px solid rgba(255,255,255,0.05)">
                        <Button colorScheme="blue" onClick={handleClose} borderRadius="full">
                            Dismiss
                        </Button>
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </Box >
    );
}

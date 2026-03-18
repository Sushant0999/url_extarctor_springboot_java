import React, { useState } from 'react';
import { Button, Card, CardBody, CardFooter, Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalFooter, Text, ModalBody, Box, Stack, Skeleton, Icon, VStack, Badge, HStack, Divider } from '@chakra-ui/react';
import { getText } from '../../apis/GetText';
import { BiText } from 'react-icons/bi';

export default function TextCard() {
    const [isOpen, setIsOpen] = useState(false);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleOpen = () => {
        setIsOpen(true);
        fetchData();
    };

    const handleClose = () => {
        setIsOpen(false);
    };

    const fetchData = async () => {
        try {
            setLoading(true);
            const content = await getText();
            setData(content);
        } catch (error) {
            console.error('Error fetching text:', error);
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
                _hover={{ borderColor: 'teal.400' }}
                transition="all 0.3s"
            >
                <CardBody p={8}>
                    <VStack align="start" spacing={4}>
                        <Box bg="teal.500" p={3} borderRadius="xl">
                            <Icon as={BiText} color="white" boxSize={6} />
                        </Box>
                        <Box>
                            <VStack align="start" spacing={0}>
                                <Text fontSize="2xl" fontWeight="bold" color="white">Paragraphs</Text>
                                <Badge colorScheme="teal" borderRadius="md" variant="subtle">CONTENT</Badge>
                            </VStack>
                        </Box>
                        <Text color="gray.400" fontSize="sm">
                            Capture all text blocks, descriptions, and article content found on the page.
                        </Text>
                    </VStack>
                </CardBody>
                <CardFooter bg="rgba(255,255,255,0.02)" borderTop="1px solid rgba(255,255,255,0.05)">
                    <Button 
                        width="100%" 
                        colorScheme="teal" 
                        variant="ghost"
                        onClick={handleOpen}
                    >
                        Read Text
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
                            <Icon as={BiText} color="teal.400" />
                            <Text>Extracted Content</Text>
                        </HStack>
                    </ModalHeader>
                    <ModalCloseButton />
                    <ModalBody py={6}>
                        {loading ? (
                            <Stack spacing={4}>
                                {[1, 2, 3, 4].map(i => (
                                    <Skeleton key={i} height="80px" borderRadius="lg" />
                                ))}
                            </Stack>
                        ) : data && data.length > 0 ? (
                            <VStack spacing={6} align="stretch">
                                {data.map((text, idx) => (
                                    <Box key={idx}>
                                        <Text fontSize="md" lineHeight="tall" color="gray.200">
                                            {text}
                                        </Text>
                                        {idx < data.length - 1 && <Divider mt={6} opacity={0.1} />}
                                    </Box>
                                ))}
                            </VStack>
                        ) : (
                            <VStack p={10}>
                                <Text color="gray.400">No text content extracted from this page.</Text>
                            </VStack>
                        )}
                    </ModalBody>
                    <ModalFooter borderTop="1px solid rgba(255,255,255,0.05)">
                        <Button colorScheme="teal" onClick={handleClose} borderRadius="full">
                            Dismiss
                        </Button>
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </Box >
    );
}

import React, { useState } from 'react';
import { Button, Card, CardBody, CardFooter, Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalFooter, Text, ModalBody, Box, Stack, Skeleton, TableContainer, Table, Thead, Tr, Th, Tbody, Td, Icon, VStack, Badge, HStack, Link } from '@chakra-ui/react';
import { getLink } from '../../apis/GetLinks';
import { BiLink, BiLinkExternal } from 'react-icons/bi';

export default function LinkCard() {
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
            const links = await getLink();
            setData(links);
        } catch (error) {
            console.error('Error fetching links:', error);
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
                _hover={{ borderColor: 'purple.400' }}
                transition="all 0.3s"
            >
                <CardBody p={8}>
                    <VStack align="start" spacing={4}>
                        <Box bg="purple.500" p={3} borderRadius="xl">
                            <Icon as={BiLink} color="white" boxSize={6} />
                        </Box>
                        <Box>
                            <VStack align="start" spacing={0}>
                                <Text fontSize="2xl" fontWeight="bold" color="white">Links</Text>
                                <Badge colorScheme="purple" borderRadius="md" variant="subtle">NAVIGATION</Badge>
                            </VStack>
                        </Box>
                        <Text color="gray.400" fontSize="sm">
                            Discover all internal and external hyperlinks found across the document.
                        </Text>
                    </VStack>
                </CardBody>
                <CardFooter bg="rgba(255,255,255,0.02)" borderTop="1px solid rgba(255,255,255,0.05)">
                    <Button 
                        width="100%" 
                        colorScheme="purple" 
                        variant="ghost"
                        onClick={handleOpen}
                    >
                        Explore Links
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
                            <Icon as={BiLink} color="purple.400" />
                            <Text>Discovered Links</Text>
                        </HStack>
                    </ModalHeader>
                    <ModalCloseButton />
                    <ModalBody py={6}>
                        {loading ? (
                            <Stack spacing={4}>
                                {[1, 2, 3, 4, 5].map(i => (
                                    <Skeleton key={i} height="50px" borderRadius="lg" />
                                ))}
                            </Stack>
                        ) : data && data.length > 0 ? (
                            <TableContainer>
                                <Table variant="simple">
                                    <Tbody>
                                        {data.map((link, idx) => (
                                            <Tr key={idx} _hover={{ bg: "rgba(255,255,255,0.02)" }}>
                                                <Td borderBottom="1px solid rgba(255,255,255,0.05)">
                                                    <HStack justifyContent="space-between">
                                                        <Link 
                                                            href={link} 
                                                            isExternal 
                                                            color="blue.300" 
                                                            fontSize="sm" 
                                                            maxW="500px" 
                                                            isTruncated
                                                        >
                                                            {link}
                                                        </Link>
                                                        <Icon as={BiLinkExternal} color="gray.500" />
                                                    </HStack>
                                                </Td>
                                            </Tr>
                                        ))}
                                    </Tbody>
                                </Table>
                            </TableContainer>
                        ) : (
                            <VStack p={10}>
                                <Text color="gray.400">No links discovered on this page.</Text>
                            </VStack>
                        )}
                    </ModalBody>
                    <ModalFooter borderTop="1px solid rgba(255,255,255,0.05)">
                        <Button colorScheme="purple" onClick={handleClose} borderRadius="full">
                            Dismiss
                        </Button>
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </Box >
    );
}

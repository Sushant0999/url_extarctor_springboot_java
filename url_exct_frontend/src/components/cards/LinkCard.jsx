import React, { useState } from 'react';
import { Button, Card, CardBody, CardFooter, Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalFooter, Text, ModalBody, Box, Stack, Skeleton, TableContainer, Table, Thead, Tr, Th, Tbody } from '@chakra-ui/react';
import { getLink } from '../../apis/GetLinks';
import image from '../../images/link.png'


export default function LinkCard() {
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
            <Card sx={{
                display: 'flex',
                textAlign: 'center',
                // backgroundImage: `url(${image})`,
                backgroundSize: 'cover',
                backgroundRepeat: 'no-repeat',
                // filter: 'blur(2px)',
            }}>
                <CardBody>
                    <Text fontSize={'30px'}>Links</Text>
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
                                    <Skeleton height='20px' />
                                    <Skeleton height='20px' />
                                    <Skeleton height='20px' />
                                    <Skeleton height='20px' />
                                </Stack>
                            ) : (
                                <TableContainer>
                                    <Table variant='simple'>
                                        <Thead>
                                            <Tr>
                                                <Th>Links</Th>
                                            </Tr>
                                        </Thead>
                                        <Tbody>
                                            {data && data.length > 0 ? (
                                                <>
                                                    {data.map((link) => (
                                                        <Text sx={{ padding: '5px' }} href={link} key={link} id={`${link}`}>{link}</Text>
                                                    ))}
                                                </>
                                            ) : (
                                                <></>
                                            )}
                                        </Tbody>
                                    </Table>
                                </TableContainer>
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

import React, { useState } from 'react';
import { Button, Card, CardBody, CardFooter, Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalFooter, Text } from '@chakra-ui/react';

export default function ImageCard() {
    const [isOpen, setIsOpen] = useState(false);

    const handleOpen = () => {
        setIsOpen(true);
    };

    const handleClose = () => {
        setIsOpen(false);
    };

    return (
        <div>
            <Card sx={{ display: 'flex', textAlign: 'center' }}>
                <CardBody>
                    <Text fontSize={'30px'}>Images</Text>
                </CardBody>
                <CardFooter sx={{ display: 'flex', justifyContent: 'center' }}>
                    <Button size='sm' onClick={handleOpen}>View here</Button>
                </CardFooter>
            </Card>

            <Modal isOpen={isOpen} onClose={handleClose}>
                <ModalOverlay />
                <ModalContent>
                    <ModalHeader>Modal Title</ModalHeader>
                    <ModalCloseButton />
                    {/* <ModalBody>
                    </ModalBody> */}
                    <ModalFooter>
                        <Button colorScheme='blue' mr={3} onClick={handleClose}>
                            Close
                        </Button>
                        <Button variant='ghost'>Secondary Action</Button>
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </div>
    );
}


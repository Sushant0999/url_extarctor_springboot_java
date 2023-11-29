import React, { useState } from 'react';
import { Modal, ModalOverlay, ModalContent, ModalHeader, ModalCloseButton, ModalBody, ModalFooter, Button } from '@chakra-ui/react';

const ModalComp = ({ title, data }) => {
    const [isOpen, setIsOpen] = useState(false);

    const onClose = () => setIsOpen(false);
    const onOpen = () => setIsOpen(true);

    return (
        <>
            <Button onClick={onOpen}>{title}</Button>

            <Modal size={'full'} isOpen={isOpen} onClose={onClose}>
                <ModalOverlay />
                <ModalContent>
                    <ModalHeader>Modal {title}</ModalHeader>
                    <ModalCloseButton />
                    <ModalBody>
                        <p>Modal Content Goes Here</p>
                        {data ? <><h1>YES</h1></> : <><h1>NO</h1></>}
                    </ModalBody>
                    <ModalFooter>
                        <Button colorScheme="blue" mr={3} onClick={onClose}>
                            Close
                        </Button>
                        {/* Additional actions can be added here */}
                    </ModalFooter>
                </ModalContent>
            </Modal>
        </>
    );
};

export default ModalComp;

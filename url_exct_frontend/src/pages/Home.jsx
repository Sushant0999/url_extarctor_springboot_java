import React from 'react'
import Navbar from '../components/Navbar'
import SearchBox from '../components/SearchBox'
import { Box, Container, Circle, VStack } from '@chakra-ui/react'

export default function Home() {
    return (
        <Box minH="100vh" position="relative" overflow="hidden">
            {/* Ambient Background Glows */}
            <Circle 
                size="800px" 
                bg="blue.900" 
                filter="blur(160px)" 
                position="absolute" 
                top="-300px" 
                right="-200px" 
                opacity="0.3"
                zIndex="0"
            />
            <Circle 
                size="600px" 
                bg="purple.900" 
                filter="blur(140px)" 
                position="absolute" 
                bottom="-200px" 
                left="-100px" 
                opacity="0.15"
                zIndex="0"
            />

            <Box position="relative" zIndex="1">
                <Navbar />
                <Container maxW="container.lg" pt={{ base: 20, md: 32 }}>
                    <VStack spacing={12}>
                        <SearchBox />
                    </VStack>
                </Container>
            </Box>
        </Box>
    )
}

import React from 'react'
import Navbar from '../components/Navbar'
import { Box, Button, SimpleGrid, Text } from '@chakra-ui/react'
import LinkCard from '../components/cards/LinkCard'
import ImageCard from '../components/cards/ImageCard'
import TextCard from '../components/cards/TextCard'
import { useNavigate } from 'react-router-dom'
import DownloadFileComponent from '../components/DownloadToast'

export default function Result() {

    const navigate = useNavigate();

    return (
        <div>
            <Navbar />
            <Box >
                <SimpleGrid spacing={4} templateColumns='repeat(auto-fill, minmax(200px, 1fr))' p={'20px'}
                >
                    <LinkCard />
                    <ImageCard />
                    <TextCard />
                </SimpleGrid>
                <Box sx={{ width: '100', display: 'flex', justifyContent: 'space-between', px: 5 }}>
                    <Button onClick={() => navigate('/')}>
                        <Text>Back</Text>
                    </Button>
                    <DownloadFileComponent />
                </Box>
            </Box>
        </div>
    )
}

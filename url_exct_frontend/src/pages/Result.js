import React from 'react'
import Navbar from '../components/Navbar'
import { Box, Button, Icon, SimpleGrid, Text } from '@chakra-ui/react'
import LinkCard from '../components/cards/LinkCard'
import ImageCard from '../components/cards/ImageCard'
import TextCard from '../components/cards/TextCard'
import { useNavigate } from 'react-router-dom'
import { DownloadIcon } from '@chakra-ui/icons'
import { getData } from '../apis/GetData'

export default function Result() {

    const navigate = useNavigate();

    const handleDownload = async () => {
        const data = await getData();
        console.log(data);
    }

    return (
        <div>
            <Navbar />
            <Box >
                <SimpleGrid spacing={1} templateColumns='repeat(auto-fill, minmax(200px, 1fr))' p={'20px'}
                >
                    <LinkCard />
                    <ImageCard />
                    <TextCard />
                </SimpleGrid>
                <Box sx={{ width: '100', display: 'flex', justifyContent: 'space-between', px: 5 }}>
                    <Button onClick={() => navigate('/')}>
                        <Text>Back</Text>
                    </Button>
                    <Button onClick={() => handleDownload()}>
                        <Icon>
                            <DownloadIcon />
                        </Icon>
                        <div style={{ width: '10px' }}></div>
                        <Text>Download</Text>
                    </Button>
                </Box>
            </Box>
        </div>
    )
}

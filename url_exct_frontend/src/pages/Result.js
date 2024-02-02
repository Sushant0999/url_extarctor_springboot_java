import React from 'react'
import Navbar from '../components/Navbar'
import { SimpleGrid } from '@chakra-ui/react'
import LinkCard from '../components/cards/LinkCard'
import ImageCard from '../components/cards/ImageCard'
import TextCard from '../components/cards/TextCard'

export default function Result() {
    return (
        <div>
            <Navbar />
            <SimpleGrid spacing={4} templateColumns='repeat(auto-fill, minmax(200px, 1fr))' p={'20px'}>
                <LinkCard />
                <ImageCard />
                <TextCard />
            </SimpleGrid>
        </div>
    )
}

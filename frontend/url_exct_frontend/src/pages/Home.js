import { Grid, GridItem } from '@chakra-ui/react'
import React from 'react'
import Navbar from '../components/Navbar'
import CardComp from '../components/cards/CardComp1'
import { getImages } from '../apis/GetImages'

export default function Home() {
    return (
        <div>
            <Navbar />
            <Grid
                templateColumns={['repeat(1, 1fr)', 'repeat(2, 1fr)', 'repeat(3, 1fr)', 'repeat(4, 1fr)', 'repeat(5, 1fr)']}
                gap={6}
                h={['auto', '200px']}
                templateRows={['repeat(1, 1fr)', 'repeat(2, 1fr)']}
            >
                <GridItem>
                    <CardComp title={'Task1'} body={'Body1'} />
                </GridItem>
                <GridItem>
                    <CardComp title={'Task2'} body={'Body2'} />
                </GridItem>
                <CardComp title={'Task3'} body={'Body3'} />
                <GridItem>
                    <CardComp title={'Task4'} body={'Body4'} />
                </GridItem>
            </Grid>
        </div>
    )
}

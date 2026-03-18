import React from 'react';
import { Box, Card, CardBody, Text, VStack, Heading, Icon, Badge, HStack, SimpleGrid, Wrap, WrapItem, Circle } from '@chakra-ui/react';
import { BiLayer, BiPalette } from 'react-icons/bi';

export default function TechStackCard({ tech, colors }) {
    return (
        <Card 
            className="premium-card"
            borderRadius="40px"
            height="100%"
        >
            <CardBody p={10}>
                <VStack align="start" spacing={10} width="100%">
                    <Box width="100%">
                        <HStack mb={5} spacing={3}>
                            <Box bg="emerald.500" p={2.5} borderRadius="14px">
                                <Icon as={BiLayer} color="white" boxSize={5} />
                            </Box>
                            <Heading size="sm" color="white" fontWeight="800" letterSpacing="wide">CORE STACK</Heading>
                        </HStack>
                        <Wrap spacing={3}>
                            {tech && tech.map((t, idx) => (
                                <WrapItem key={idx}>
                                    <Badge bg="whiteAlpha.100" color="emerald.300" border="1px solid rgba(16, 185, 129, 0.2)" px={4} py={1.5} borderRadius="12px" fontSize="10px" fontWeight="800">
                                        {t.toUpperCase()}
                                    </Badge>
                                </WrapItem>
                            ))}
                            {(!tech || tech.length === 0) && <Text color="gray.500" fontSize="xs">No major frameworks detected.</Text>}
                        </Wrap>
                    </Box>

                    <Box width="100%">
                        <HStack mb={5} spacing={3}>
                            <Box bg="indigo.500" p={2.5} borderRadius="14px">
                                <Icon as={BiPalette} color="white" boxSize={5} />
                            </Box>
                            <Heading size="sm" color="white" fontWeight="800" letterSpacing="wide">VISUAL DNA</Heading>
                        </HStack>
                        <HStack spacing={6}>
                            {colors && colors.map((color, idx) => (
                                <VStack key={idx} spacing={2}>
                                    <Circle size="40px" bg={color} border="2px solid rgba(255,255,255,0.1)" boxShadow="xl" />
                                    <Text fontSize="9px" color="gray.500" fontWeight="bold" letterSpacing="0.05em">{color.toUpperCase()}</Text>
                                </VStack>
                            ))}
                        </HStack>
                    </Box>
                </VStack>
            </CardBody>
        </Card>
    );
}

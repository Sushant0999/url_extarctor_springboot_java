import React from 'react';
import { Box, Card, CardBody, Text, VStack, Heading, Icon, Badge, HStack, Divider } from '@chakra-ui/react';
import { BiFile } from 'react-icons/bi';

export default function SummaryCard({ summary }) {
    return (
        <Card 
            className="premium-card"
            borderRadius="40px"
            height="100%"
            overflow="hidden"
        >
            <CardBody p={10}>
                <VStack align="start" spacing={6}>
                    <HStack width="100%" justifyContent="space-between">
                        <Box bgGradient="linear(to-br, #818cf8, #6366f1)" p={3.5} borderRadius="22px">
                            <Icon as={BiFile} color="white" boxSize={6} />
                        </Box>
                        <Badge colorScheme="indigo" variant="subtle" px={3} py={1} borderRadius="full" fontSize="10px" letterSpacing="0.1em">EXECUTIVE SUMMARY</Badge>
                    </HStack>
                    
                    <Box>
                        <Heading size="md" color="white" mb={4} fontWeight="800">Page Context Analysis</Heading>
                        <Text color="gray.300" fontSize="lg" fontStyle="italic" lineHeight="1.8" fontWeight="500">
                            "{summary || "Synthesizing page content analysis..."}"
                        </Text>
                    </Box>
                </VStack>
            </CardBody>
        </Card>
    );
}

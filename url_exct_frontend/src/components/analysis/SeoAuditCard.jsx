import React from 'react';
import { Box, Card, CardBody, Text, VStack, Heading, Icon, Badge, HStack, SimpleGrid, Flex } from '@chakra-ui/react';
import { BiCheckCircle, BiErrorCircle, BiCheckShield } from 'react-icons/bi';

export default function SeoAuditCard({ issues }) {
    return (
        <Card 
            className="premium-card"
            borderRadius="40px"
            overflow="hidden"
            bg="transparent"
        >
            <CardBody p={10}>
                <VStack align="start" spacing={10} width="100%">
                    <Flex width="100%" justify="space-between" align="center">
                        <HStack spacing={4}>
                            <Box bgGradient="linear(to-br, #f472b6, #db2777)" p={3} borderRadius="18px">
                                <Icon as={BiCheckShield} color="white" boxSize={6} />
                            </Box>
                            <VStack align="start" spacing={0}>
                                <Heading size="md" color="white" fontWeight="800">SEO Integrity Scan</Heading>
                                <Text color="gray.500" fontSize="xs" fontWeight="700" letterSpacing="widest">DIAGNOSTIC ANALYSIS</Text>
                            </VStack>
                        </HStack>
                    </Flex>

                    <SimpleGrid columns={{ base: 1, md: 2, lg: 3 }} spacing={10} width="100%">
                        {issues && Object.entries(issues).map(([key, issue]) => (
                            <HStack key={key} spacing={5} p={6} bg="whiteAlpha.50" borderRadius="24px" border="1px solid rgba(255,255,255,0.05)" transition="all 0.3s" _hover={{ bg: "whiteAlpha.100", transform: "translateY(-4px)" }}>
                                <Icon 
                                    as={issue.status === 'PASS' ? BiCheckCircle : BiErrorCircle} 
                                    color={issue.status === 'PASS' ? "emerald.400" : "pink.400"} 
                                    boxSize={6} 
                                />
                                <VStack align="start" spacing={1}>
                                    <Text color="gray.300" fontWeight="800" fontSize="xs" letterSpacing="0.05em">{key.replace(/([A-Z])/g, ' $1').toUpperCase()}</Text>
                                    <Badge 
                                        colorScheme={issue.status === 'PASS' ? 'emerald' : 'pink'} 
                                        variant="subtle" 
                                        px={2} 
                                        py={0.5} 
                                        borderRadius="md" 
                                        fontSize="9px"
                                        fontWeight="900"
                                    >
                                        {issue.status}
                                    </Badge>
                                </VStack>
                            </HStack>
                        ))}
                    </SimpleGrid>
                </VStack>
            </CardBody>
        </Card>
    );
}

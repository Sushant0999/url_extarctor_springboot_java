import React, { useEffect, useState } from 'react'
import Navbar from '../components/Navbar'
import { Box, Button, SimpleGrid, Text, Container, VStack, Heading, Icon, Flex, Image, Tabs, TabList, TabPanels, Tab, TabPanel, Badge, HStack, Circle } from '@chakra-ui/react'
import LinkCard from '../components/cards/LinkCard'
import ImageCard from '../components/cards/ImageCard'
import TextCard from '../components/cards/TextCard'
import SummaryCard from '../components/analysis/SummaryCard'
import SeoAuditCard from '../components/analysis/SeoAuditCard'
import TechStackCard from '../components/analysis/TechStackCard'
import { useNavigate } from 'react-router-dom'
import DownloadFileComponent from '../components/DownloadToast'
import { BiArrowBack, BiScreenshot, BiData, BiShieldQuarter } from 'react-icons/bi'
import { motion } from 'framer-motion'

export default function Result() {
    const navigate = useNavigate();
    const [data, setData] = useState(null);

    useEffect(() => {
        const stored = localStorage.getItem('lastExtraction');
        if (stored) {
            setData(JSON.parse(stored));
        } else {
            navigate('/');
        }
    }, [navigate]);

    if (!data) return null;

    return (
        <Box minH="100vh" pb={24} position="relative" overflow="hidden" bg="transparent">
            {/* Advanced Ambient background */}
            <Circle size="700px" bg="indigo.900" filter="blur(160px)" position="absolute" top="-150px" left="-150px" opacity="0.2" zIndex="-1" />
            <Circle size="500px" bg="emerald.900" filter="blur(140px)" position="absolute" top="20%" right="-100px" opacity="0.1" zIndex="-1" />
            <Circle size="600px" bg="purple.900" filter="blur(150px)" position="absolute" bottom="-100px" left="20%" opacity="0.1" zIndex="-1" />

            <Navbar />
            
            <Container maxW="container.xl" pt={{ base: 10, md: 24 }}>
                <VStack spacing={{ base: 10, md: 16 }} align="stretch" className="animate-fade-in">
                    
                    {/* Diagnostic Header */}
                    <Flex 
                        direction={{ base: 'column', lg: 'row' }} 
                        justify="space-between" 
                        align={{ base: 'start', lg: 'center' }} 
                        gap={8}
                    >
                        <VStack align="start" spacing={4}>
                            <HStack spacing={3}>
                                <Badge px={3} py={1} borderRadius="full" bg="whiteAlpha.100" color="indigo.300" border="1px solid rgba(129, 140, 248, 0.2)" fontSize="xs" fontWeight="bold">
                                    REPORT ID: #{Math.floor(Math.random() * 90000) + 10000}
                                </Badge>
                                <Text color="gray.500" fontSize="sm" fontWeight="600" isTruncated maxW="400px">
                                    {data.baseUrl}
                                </Text>
                            </HStack>
                            <Heading 
                                fontSize={{ base: "3xl", md: "5xl" }} 
                                color="white" 
                                fontWeight="900" 
                                letterSpacing="tight"
                                className="text-gradient"
                            >
                                {data.title || "Extraction Summary"}
                            </Heading>
                        </VStack>
                        
                        <HStack spacing={4} width={{ base: 'full', lg: 'auto' }}>
                            <Button 
                                leftIcon={<BiArrowBack />} 
                                variant="outline" 
                                color="gray.400" 
                                borderColor="whiteAlpha.100"
                                _hover={{ bg: 'whiteAlpha.100', color: 'white', borderColor: 'indigo.400' }}
                                onClick={() => navigate('/')}
                                borderRadius="20px"
                                size="lg"
                                px={8}
                                fontWeight="700"
                            >
                                START OVER
                            </Button>
                            <DownloadFileComponent />
                        </HStack>
                    </Flex>

                    {/* Dashboard Control Architecture */}
                    <Tabs variant="unstyled">
                        <TabList 
                            bg="rgba(255,255,255,0.02)" 
                            p={2} 
                            borderRadius="24px" 
                            border="1px solid rgba(255,255,255,0.05)" 
                            width="fit-content" 
                            mb={12}
                            display="flex"
                            gap={3}
                        >
                            {[
                                { name: 'Insights', icon: BiData },
                                ...(data.screenshotBase64 ? [{ name: 'Snapshot', icon: BiScreenshot }] : []),
                                { name: 'Asset Map', icon: BiShieldQuarter }
                            ].map((tab) => (
                                <Tab
                                    key={tab.name}
                                    px={10}
                                    py={4}
                                    borderRadius="18px"
                                    color="gray.500"
                                    fontSize="xs"
                                    fontWeight="800"
                                    letterSpacing="0.1em"
                                    textTransform="uppercase"
                                    _selected={{ 
                                        color: 'white', 
                                        bg: 'indigo.600',
                                        boxShadow: '0 8px 25px rgba(99, 102, 241, 0.3)' 
                                    }}
                                    _hover={{ color: 'whiteAlpha.800' }}
                                    transition="all 0.4s cubic-bezier(0.16, 1, 0.3, 1)"
                                >
                                    <HStack spacing={3}>
                                        <Icon as={tab.icon} boxSize={4} />
                                        <Text>{tab.name}</Text>
                                    </HStack>
                                </Tab>
                            ))}
                        </TabList>

                        <TabPanels>
                            <TabPanel p={0}>
                                <SimpleGrid columns={{ base: 1, lg: 3 }} spacing={10}>
                                    <Box gridColumn={{ lg: "span 2" }}>
                                        <SummaryCard summary={data.summary} />
                                    </Box>
                                    <TechStackCard tech={data.techStack} colors={data.colorPalette} />
                                    <Box gridColumn={{ lg: "span 3" }}>
                                        <SeoAuditCard issues={data.seoIssues} />
                                    </Box>
                                </SimpleGrid>
                            </TabPanel>

                            {data.screenshotBase64 && (
                                <TabPanel p={0}>
                                    <Box 
                                        borderRadius="40px" 
                                        overflow="hidden" 
                                        className="glass-effect" 
                                        p={4}
                                        bg="blackAlpha.600"
                                        border="1px solid rgba(129, 140, 248, 0.1)"
                                    >
                                        <Image 
                                            src={`data:image/png;base64,${data.screenshotBase64}`} 
                                            alt="Visual Analysis" 
                                            borderRadius="32px"
                                            width="100%"
                                            maxH="1000px"
                                            objectFit="contain"
                                            transition="transform 0.5s ease"
                                            _hover={{ transform: 'scale(1.01)' }}
                                            loading="lazy"
                                        />
                                    </Box>
                                </TabPanel>
                            )}

                            <TabPanel p={0}>
                                <SimpleGrid columns={{ base: 1, md: 2, lg: 3 }} spacing={8}>
                                    <motion.div whileHover={{ y: -12 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
                                        <LinkCard />
                                    </motion.div>
                                    <motion.div whileHover={{ y: -12 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
                                        <ImageCard />
                                    </motion.div>
                                    <motion.div whileHover={{ y: -12 }} transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}>
                                        <TextCard />
                                    </motion.div>
                                </SimpleGrid>
                            </TabPanel>
                        </TabPanels>
                    </Tabs>
                </VStack>
            </Container>
        </Box>
    )
}

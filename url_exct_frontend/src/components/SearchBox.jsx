import { Box, Button, Input, Switch, useToast, Text, VStack, HStack, Progress, Fade, ScaleFade, Badge, Heading } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import { addLinks } from '../apis/AddLinks';
import { ScatterBoxLoaderComponent } from './loaders/ScatterBoxLoaderComponent';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';

const steps = [
    "Validating URL structure...",
    "Connecting to extraction engine...",
    "Analyzing page metadata...",
    "Scanning for media content...",
    "Extracting links and assets...",
    "Finalizing data package..."
];

export default function SearchBox() {
    const navigate = useNavigate();
    const [inputText, setInputText] = useState('');
    const [isLoading, setLoading] = useState(false);
    const [enable, isEnable] = useState(true);
    const [currentStep, setCurrentStep] = useState(0);

    const toast = useToast();

    useEffect(() => {
        isEnable(true);
    }, []);

    useEffect(() => {
        let interval;
        if (isLoading) {
            setCurrentStep(0);
            interval = setInterval(() => {
                setCurrentStep(prev => (prev < steps.length - 1 ? prev + 1 : prev));
            }, 1500);
        }
        return () => clearInterval(interval);
    }, [isLoading]);

    const handleSearch = async () => {
        if (!inputText) {
            toast({
                description: 'Enter a URL to analyze',
                status: 'info',
                variant: 'subtle',
                position: 'top-right'
            });
            return;
        }

        setLoading(true);
        try {
            const response = await addLinks(inputText, enable);
            if (response.status === 200) {
                localStorage.setItem('lastExtraction', JSON.stringify(response.data));
                setCurrentStep(steps.length - 1);
                setTimeout(() => {
                    navigate('/result');
                }, 1000);
            } else {
                setLoading(false);
                toast({ title: 'Extraction failed', status: 'error' });
            }
        } catch (error) {
            setLoading(false);
            console.error(error);
        }
    };

    return (
        <Box 
            minH="60vh" 
            display="flex" 
            flexDirection="column" 
            justifyContent="center" 
            alignItems="center"
            px={4}
        >
            <AnimatePresence mode="wait">
                {isLoading ? (
                    <motion.div
                        key="loader"
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 1.05 }}
                        style={{ width: '100%', maxWidth: '450px' }}
                    >
                        <VStack spacing={10} className="glass-effect" p={12} borderRadius="32px" border="1px solid rgba(129, 140, 248, 0.2)">
                            <ScatterBoxLoaderComponent />
                            <VStack spacing={5} width="100%">
                                <Text fontSize="lg" fontWeight="600" color="indigo.200" letterSpacing="wide">
                                    {steps[currentStep]}
                                </Text>
                                <Progress 
                                    value={((currentStep + 1) / steps.length) * 100} 
                                    size="xs" 
                                    colorScheme="indigo" 
                                    bg="whiteAlpha.50"
                                    width="100%" 
                                    borderRadius="full"
                                    isAnimated
                                />
                                <HStack width="100%" justifyContent="space-between">
                                    <Badge variant="subtle" colorScheme="indigo" fontSize="10px" borderRadius="md" px={2}>STEP {currentStep + 1}</Badge>
                                    <Text fontSize="xs" fontWeight="700" color="indigo.300">{Math.round(((currentStep + 1) / steps.length) * 100)}%</Text>
                                </HStack>
                            </VStack>
                        </VStack>
                    </motion.div>
                ) : (
                    <motion.div
                        key="search"
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95 }}
                        style={{ width: '100%', maxWidth: '700px' }}
                        transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
                    >
                        <VStack 
                            spacing={10} 
                            p={12} 
                            className="glass-effect" 
                            borderRadius="40px"
                            border="1px solid rgba(255,255,255,0.08)"
                        >
                            <VStack spacing={4}>
                                <Badge colorScheme="emerald" variant="outline" px={3} py={0.5} borderRadius="full" fontSize="10px" letterSpacing="widest">
                                    NEXT GEN EXTRACTION
                                </Badge>
                                <Heading 
                                    fontSize={{ base: "4xl", md: "6xl" }} 
                                    fontWeight="900" 
                                    className="text-gradient"
                                    letterSpacing="tight"
                                    lineHeight="shorter"
                                >
                                    Deep Web Analyzer
                                </Heading>
                                <Text color="gray.400" fontSize="lg" maxW="450px" textAlign="center" fontWeight="500">
                                    Synthesize structured data, visual assets, and SEO insights from any URL.
                                </Text>
                            </VStack>

                            <VStack width="100%" spacing={6}>
                                <Box width="100%" position="relative">
                                    <Input
                                        placeholder="Enter target URL (e.g. google.com)"
                                        value={inputText}
                                        onChange={(e) => setInputText(e.target.value)}
                                        size="lg"
                                        variant="unstyled"
                                        px={8}
                                        py={8}
                                        bg="whiteAlpha.50"
                                        border="1px solid rgba(255,255,255,0.1)"
                                        _focus={{ bg: "whiteAlpha.100", borderColor: "indigo.400", boxShadow: "0 0 30px rgba(129, 140, 248, 0.1)" }}
                                        borderRadius="24px"
                                        fontSize="md"
                                        color="white"
                                        fontWeight="600"
                                    />
                                </Box>
                                
                                <HStack width="100%" justifyContent="space-between" wrap="wrap" gap={4}>
                                    <HStack bg="whiteAlpha.50" px={4} py={2} borderRadius="xl" border="1px solid rgba(255,255,255,0.05)">
                                        <Text fontSize="xs" fontWeight="700" color="gray.400" letterSpacing="0.05em">JS ENGINE</Text>
                                        <Switch 
                                            isChecked={enable} 
                                            onChange={(e) => isEnable(e.target.checked)} 
                                            colorScheme="indigo" 
                                        />
                                    </HStack>
                                    <Button 
                                        bgGradient="linear(to-r, #818cf8, #6366f1)"
                                        color="white"
                                        size="lg" 
                                        h="64px"
                                        px={12}
                                        onClick={handleSearch}
                                        borderRadius="20px"
                                        fontSize="md"
                                        fontWeight="800"
                                        letterSpacing="wide"
                                        boxShadow="0 10px 40px -10px rgba(99, 102, 241, 0.5)"
                                        _hover={{ transform: 'translateY(-4px)', boxShadow: "0 15px 50px -10px rgba(99, 102, 241, 0.6)" }}
                                        _active={{ transform: 'translateY(0)' }}
                                        transition="all 0.3s cubic-bezier(0.16, 1, 0.3, 1)"
                                    >
                                        ANALYZE NOW
                                    </Button>
                                </HStack>
                            </VStack>
                        </VStack>
                    </motion.div>
                )}
            </AnimatePresence>
        </Box>
    );
}

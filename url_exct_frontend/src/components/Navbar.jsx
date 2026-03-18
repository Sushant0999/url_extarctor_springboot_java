import { Box, Flex, Text, HStack, Icon, Container, Link } from '@chakra-ui/react';
import { BiLinkExternal } from 'react-icons/bi';
import { motion } from 'framer-motion';

export default function Nav() {
  return (
    <Box 
      as="nav" 
      position="fixed" 
      top="0" 
      zIndex="100" 
      w="100%" 
      className="glass-effect"
      borderBottom="1px solid rgba(255,255,255,0.05)"
      backdropFilter="blur(30px) saturate(200%)"
    >
      <Container maxW="container.xl">
        <Flex h={16} alignItems="center" justifyContent="space-between">
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
          >
            <HStack spacing={4} cursor="pointer" onClick={() => window.location.href='/'}>
              <Box 
                bgGradient="linear(to-br, #818cf8, #6366f1)" 
                p={1.5} 
                borderRadius="xl"
                boxShadow="0 4px 15px rgba(129, 140, 248, 0.3)"
              >
                <Icon as={BiLinkExternal} color="white" boxSize={5} />
              </Box>
              <Text 
                fontSize="lg" 
                fontWeight="800" 
                letterSpacing="tight"
                color="white"
              >
                URL<Text as="span" bgGradient="linear(to-r, #818cf8, #10b981)" bgClip="text" backgroundClip="text">X</Text>
              </Text>
            </HStack>
          </motion.div>

          <HStack spacing={10} display={{ base: 'none', md: 'flex' }}>
            {['Engine', 'Features', 'API'].map((item) => (
              <Link
                key={item}
                fontSize="xs"
                fontWeight="700"
                color="gray.400"
                letterSpacing="0.1em"
                textTransform="uppercase"
                _hover={{ color: "white", textDecoration: "none" }}
                transition="all 0.3s ease"
              >
                {item}
              </Link>
            ))}
          </HStack>

          <Box />
        </Flex>
      </Container>
    </Box>
  );
}

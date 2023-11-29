import { Box, Card, CardBody, Stack } from '@chakra-ui/react';
import React from 'react';
import ModalComp from '../modals/ModalComp';

export default function CardComp({ title, body, data }) {

  return (
    <Stack spacing="true" sx={{ padding: '10px', margin: '10px' }}>
      <Card>
        {/* <CardHeader>
          <Text>{title}</Text>
        </CardHeader> */}
        <CardBody>
          <Box h="100px">
            <p>{body}</p>
          </Box>
        </CardBody>
        <ModalComp title={title} />
      </Card>
    </Stack>
  );
}
